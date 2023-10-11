/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.airlift.stats.cardinality;

import com.google.common.annotations.VisibleForTesting;
import io.airlift.slice.BasicSliceInput;
import io.airlift.slice.DynamicSliceOutput;
import io.airlift.slice.SizeOf;
import io.airlift.slice.Slice;

import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A privacy-preserving cardinality sketch similar in spirit to HyperLogLog.
 * This is essentially a data sketch that has buckets similar to those of HLL,
 * but converted to binary values (above/below) relative to some threshold.
 * This serves as a sort of data minimization. Additional noise is added to the
 * sketch by randomly perturbing the buckets via randomized response.
 * The final cardinality estimate is similar to the classical linear probabilistic
 * counting algorithm (LPCA).
 *
 * <p>The final sketch (and the resulting cardinality estimate) is
 * epsilon-differentially private, where
 * epsilon = epsilonThreshold + epsilonRandomizedResponse.
 */
@NotThreadSafe
public class PrivateLpcaSketch
{
    private Bitmap bitmap;
    private final int threshold;
    private final int numberOfBuckets;
    private final double epsilonThreshold;
    private final double epsilonRandomizedResponse;
    private final RandomizationStrategy randomizationStrategy;
    private static final int BYTE_MASK = 0b11111111;

    public PrivateLpcaSketch(HyperLogLog hll, double epsilonThreshold, double epsilonRandomizedResponse)
    {
        this(hll, epsilonThreshold, epsilonRandomizedResponse, new SecureRandomizationStrategy());
    }

    public PrivateLpcaSketch(Slice serialized)
    {
        this(serialized, new SecureRandomizationStrategy());
    }

    public PrivateLpcaSketch(HyperLogLog hll, double epsilonThreshold, double epsilonRandomizedResponse, RandomizationStrategy randomizationStrategy)
    {
        this.randomizationStrategy = randomizationStrategy;
        this.epsilonThreshold = epsilonThreshold;
        this.epsilonRandomizedResponse = epsilonRandomizedResponse;
        numberOfBuckets = hll.getNumberOfBuckets();
        threshold = findThreshold(hll);
        writeBitmap(hll);
        applyRandomizedResponse();
    }

    public PrivateLpcaSketch(Slice serialized, RandomizationStrategy randomizationStrategy)
    {
        this.randomizationStrategy = randomizationStrategy;

        // Format:
        // format | numberOfBuckets | threshold | epsilon | bitmap
        BasicSliceInput input = serialized.getInput();
        byte format = input.readByte();
        checkArgument(format == Format.PRIVATE_LPCA_V1.getTag(), "Wrong format tag");

        numberOfBuckets = input.readInt();
        threshold = input.readInt();
        epsilonThreshold = input.readDouble();
        epsilonRandomizedResponse = input.readDouble();
        bitmap = Bitmap.fromSliceInput(input, numberOfBuckets);
    }

    private void applyRandomizedResponse()
    {
        double p = getFlipProbability();
        for (int i = 0; i < numberOfBuckets; i++) {
            if (randomizationStrategy.nextBoolean(p)) {
                bitmap.flipBit(i);
            }
        }
    }

    private void applyRandomizedResponse(int bucket)
    {
        double p = getFlipProbability();
        if (randomizationStrategy.nextBoolean(p)) {
            bitmap.flipBit(bucket);
        }
    }

    public long cardinality()
    {
        double proportion = getDebiasedBitProportion();
        double estimate = -Math.pow(2.0, threshold) * Math.log1p(-proportion) * numberOfBuckets;
        return Math.round(estimate);
    }

    public int estimatedSerializedSize()
    {
        return SizeOf.SIZE_OF_BYTE + // type + version
                SizeOf.SIZE_OF_INT + // number of buckets
                SizeOf.SIZE_OF_INT + // threshold
                SizeOf.SIZE_OF_DOUBLE + // epsilonThreshold
                SizeOf.SIZE_OF_DOUBLE + // epsilonRandomizedResponse
                (numberOfBuckets / Byte.SIZE * SizeOf.SIZE_OF_BYTE); // bitmap
    }

    private int findThreshold(HyperLogLog hll)
    {
        // We use a noisy adjusted mean as a measure of centrality.
        // Empirically, the mean seems to be around 0.20 higher than the median on average.
        // This means that usually, we should be picking a value very close to the median.
        int[] values = new int[hll.getNumberOfBuckets()];
        hll.eachBucket((i, value) -> values[i] = value);
        double mean = 0;
        for (int val : values) {
            mean += (double) val / hll.getNumberOfBuckets();
        }

        double sensitivity = (double) hll.getMaxBucketValue() / hll.getNumberOfBuckets();
        double noise = randomizationStrategy.nextLaplace(sensitivity / epsilonThreshold);
        int result = (int) Math.round(mean + noise - 0.2);
        return Math.min(hll.getMaxBucketValue() - 1, Math.max(0, result)); // ensure within sane bounds
    }

    @VisibleForTesting
    Bitmap getBitmap()
    {
        return bitmap;
    }

    private double getDebiasedBitProportion()
    {
        // Each bit has expectation:
        // p + (1-2p) T_i,
        // where T_i is the true bit value, and p is the (effective) flip probability.
        // So the proportion of bits equal to 1 has expectation:
        // p + (1-2p) T,
        // where T is the true proportion.
        double probability = getFlipProbability();
        return (getRawBitProportion() - probability) / (1 - 2 * probability);
    }

    private double getFlipProbability()
    {
        return 1.0 / (Math.exp(epsilonRandomizedResponse) + 1.0);
    }

    public int getNumberOfBuckets()
    {
        return numberOfBuckets;
    }

    @VisibleForTesting
    double getRawBitProportion()
    {
        return (double) bitmap.getBitCount() / numberOfBuckets;
    }

    public int getThreshold()
    {
        return threshold;
    }

    public Slice serialize()
    {
        int size = estimatedSerializedSize();

        DynamicSliceOutput output = new DynamicSliceOutput(size)
                .appendByte(Format.PRIVATE_LPCA_V1.getTag())
                .appendInt(numberOfBuckets)
                .appendInt(threshold)
                .appendDouble(epsilonThreshold)
                .appendDouble(epsilonRandomizedResponse)
                .appendBytes(bitmap.toBytes());

        return output.slice();
    }

    /**
     * Updates current sketch by adding data from a second HyperLogLog
     *
     * @param hllOther HyperLogLog of data to add to current sketch
     */
    public void update(HyperLogLog hllOther)
    {
        checkArgument(hllOther.getNumberOfBuckets() == numberOfBuckets,
                "Cannot update sketch using HyperLogLog with different number of buckets: %s vs %s", numberOfBuckets, hllOther.getNumberOfBuckets());

        hllOther.eachBucket((i, value) -> {
            // if the new HLL's bucket value is at or below threshold, we don't need to do anything
            // if above threshold, we need to set to 1 and then re-apply randomized response on the bit
            if (value > threshold) {
                bitmap.setBit(i, true);
                applyRandomizedResponse(i);
            }
        });
    }

    private void writeBitmap(HyperLogLog hll)
    {
        bitmap = new Bitmap(numberOfBuckets);
        hll.eachBucket((i, value) -> bitmap.setBit(i, value > threshold));
    }
}
