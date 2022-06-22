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

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A privacy-preserving cardinality sketch similar in spirit to HyperLogLog.
 * This is essentially a data sketch that has buckets similar to those of HLL,
 * but converted to binary values (above/below) relative to some threshold.
 * This serves as a sort of data minimization. Additional noise is added to the
 * sketch by randomly perturbing the buckets via randomized response.
 * The final cardinality estimate is similar to the classical linear probabilistic
 * counting algorithm (LPCA).
 */
@NotThreadSafe
public class PrivateLpcaSketch
{
    private byte[] bitmap;
    private final int threshold;
    private final int numberOfBuckets;
    private final double epsilon;
    private final RandomizedResponseStrategy randomizedResponseStrategy;
    private static final int BYTE_MASK = 0b11111111;

    public PrivateLpcaSketch(HyperLogLog hll, double epsilon)
    {
        this(hll, epsilon, new SecureRandomizedResponse());
    }

    public PrivateLpcaSketch(Slice serialized)
    {
        this(serialized, new SecureRandomizedResponse());
    }

    public PrivateLpcaSketch(HyperLogLog hll, double epsilon, RandomizedResponseStrategy randomizedResponseStrategy)
    {
        this.randomizedResponseStrategy = randomizedResponseStrategy;
        this.epsilon = epsilon;

        numberOfBuckets = hll.getNumberOfBuckets();
        threshold = findThreshold(hll);
        writeBitmap(hll);
        applyRandomizedResponse();
    }

    public PrivateLpcaSketch(Slice serialized, RandomizedResponseStrategy randomizedResponseStrategy)
    {
        this.randomizedResponseStrategy = randomizedResponseStrategy;

        // Format:
        // format | numberOfBuckets | threshold | epsilon | bitmap
        BasicSliceInput input = serialized.getInput();
        byte format = input.readByte();
        checkArgument(format == Format.PRIVATE_LPCA_V1.getTag(), "Wrong format tag");

        numberOfBuckets = input.readInt();
        threshold = input.readInt();
        epsilon = input.readDouble();
        bitmap = new byte[numberOfBuckets / Byte.SIZE];
        for (int i = 0; i < bitmap.length; i++) {
            bitmap[i] = input.readByte();
        }
    }

    private void applyRandomizedResponse()
    {
        double p = getFlipProbability();
        for (int i = 0; i < numberOfBuckets; i++) {
            if (randomizedResponseStrategy.shouldFlip(p)) {
                flipBit(i);
            }
        }
    }

    private void applyRandomizedResponse(int bucket)
    {
        double p = getFlipProbability();
        if (randomizedResponseStrategy.shouldFlip(p)) {
            flipBit(bucket);
        }
    }

    @VisibleForTesting
    static int bitmapBitShift(int bucket)
    {
        return bucket % Byte.SIZE;
    }

    @VisibleForTesting
    static int bitmapByteIndex(int bucket)
    {
        // n.b.: bucket is 0-indexed
        return Math.floorDiv(bucket, Byte.SIZE);
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
                SizeOf.SIZE_OF_DOUBLE + // epsilon
                (numberOfBuckets / Byte.SIZE * SizeOf.SIZE_OF_BYTE); // bitmap
    }

    private static int findThreshold(HyperLogLog hll)
    {
        // This function is subject to change.
        int[] values = new int[hll.getNumberOfBuckets()];
        hll.eachBucket((i, value) -> values[i] = value);
        Arrays.sort(values);
        return values[Math.floorDiv(values.length, 2)];
    }

    @VisibleForTesting
    void flipBit(int bucket)
    {
        byte oneBit = (byte) (1 << bitmapBitShift(bucket));
        bitmap[bitmapByteIndex(bucket)] ^= oneBit;
    }

    @VisibleForTesting
    byte[] getBitmap()
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
        double effProbability = randomizedResponseStrategy.effectiveProbability(getFlipProbability());
        return (getRawBitProportion() - effProbability) / (1 - 2 * effProbability);
    }

    private double getFlipProbability()
    {
        return 1.0 / (Math.exp(epsilon) + 1.0);
    }

    public int getNumberOfBuckets()
    {
        return numberOfBuckets;
    }

    @VisibleForTesting
    double getRawBitProportion()
    {
        double count = 0;
        for (byte b : bitmap) {
            count += Integer.bitCount(b & BYTE_MASK);
        }
        return count / numberOfBuckets;
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
                .appendDouble(epsilon)
                .appendBytes(bitmap);

        return output.slice();
    }

    @VisibleForTesting
    void setBit(int bucket, boolean value)
    {
        byte oneBit = (byte) (1 << bitmapBitShift(bucket));
        if (value) {
            bitmap[bitmapByteIndex(bucket)] |= oneBit;
        }
        else {
            bitmap[bitmapByteIndex(bucket)] &= ~oneBit;
        }
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
                setBit(i, true);
                applyRandomizedResponse(i);
            }
        });
    }

    private void writeBitmap(HyperLogLog hll)
    {
        bitmap = new byte[numberOfBuckets / Byte.SIZE];
        hll.eachBucket((i, value) -> setBit(i, value > threshold));
    }
}
