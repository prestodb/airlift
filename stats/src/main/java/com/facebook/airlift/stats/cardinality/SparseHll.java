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
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import io.airlift.slice.BasicSliceInput;
import io.airlift.slice.DynamicSliceOutput;
import io.airlift.slice.SizeOf;
import io.airlift.slice.Slice;
import org.openjdk.jol.info.ClassLayout;

import javax.annotation.concurrent.NotThreadSafe;

import java.util.Arrays;

import static com.facebook.airlift.stats.cardinality.Utils.computeIndex;
import static com.facebook.airlift.stats.cardinality.Utils.linearCounting;
import static com.facebook.airlift.stats.cardinality.Utils.numberOfBuckets;
import static com.facebook.airlift.stats.cardinality.Utils.numberOfLeadingZeros;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.airlift.slice.SizeOf.sizeOf;
import static java.lang.Math.toIntExact;
import static java.util.Comparator.comparingInt;

@NotThreadSafe
final class SparseHll
        implements HllInstance
{
    private static final int SPARSE_INSTANCE_SIZE = ClassLayout.parseClass(SparseHll.class).instanceSize();

    // 6 bits to encode the number of zeros after the truncated hash
    // and be able to fit the encoded value in an integer
    private static final int VALUE_BITS = 6;
    private static final int VALUE_MASK = (1 << VALUE_BITS) - 1;
    private static final int EXTENDED_PREFIX_BITS = Integer.SIZE - VALUE_BITS;

    private final byte indexBitLength;
    private short numberOfEntries;
    private int[] entries;

    public SparseHll(int indexBitLength)
    {
        validatePrefixLength(indexBitLength);

        this.indexBitLength = (byte) indexBitLength;
        entries = new int[1];
    }

    public SparseHll(Slice serialized)
    {
        BasicSliceInput input = serialized.getInput();

        checkArgument(input.readByte() == Format.SPARSE_V2.getTag(), "invalid format tag");

        indexBitLength = input.readByte();
        validatePrefixLength(indexBitLength);

        numberOfEntries = input.readShort();

        entries = new int[numberOfEntries];
        for (int i = 0; i < numberOfEntries; i++) {
            entries[i] = input.readInt();
        }

        checkArgument(!input.isReadable(), "input is too big");
    }

    public static boolean canDeserialize(Slice serialized)
    {
        return serialized.getByte(0) == Format.SPARSE_V2.getTag();
    }

    public void insertHash(long hash)
    {
        // TODO: investigate whether accumulate, sort and merge results in better performance due to avoiding the shift+insert in every call

        int bucket = Utils.computeIndex(hash, indexBitLength);
        int position = searchBucket(bucket);

        // add entry if missing
        if (position < 0) {
            // ensure capacity
            if (numberOfEntries + 1 > entries.length) {
                entries = Arrays.copyOf(entries, entries.length + 10);
            }

            // shift right
            int insertionPoint = -(position + 1);
            if (insertionPoint < numberOfEntries) {
                System.arraycopy(entries, insertionPoint, entries, insertionPoint + 1, numberOfEntries - insertionPoint);
            }

            entries[insertionPoint] = encode(hash);
            numberOfEntries++;
        }
        else {
            int currentEntry = entries[position];
            int newValue = Utils.numberOfLeadingZeros(hash, indexBitLength);

            if (decodeBucketValue(currentEntry) < newValue) {
                entries[position] = encode(bucket, newValue);
            }
        }
    }

    private int encode(long hash)
    {
        return encode(computeIndex(hash, indexBitLength), numberOfLeadingZeros(hash, indexBitLength));
    }

    @VisibleForTesting
    static int encode(int bucketIndex, int value)
    {
        return (bucketIndex << VALUE_BITS) | value;
    }

    @VisibleForTesting
    static int decodeBucketIndex(int entry)
    {
        return entry >>> VALUE_BITS;
    }

    @VisibleForTesting
    static int decodeBucketValue(int entry)
    {
        return entry & VALUE_MASK;
    }

    public void mergeWith(SparseHll other)
    {
        entries = mergeEntries(other);
        numberOfEntries = (short) entries.length;
    }

    public DenseHll toDense()
    {
        DenseHll result = new DenseHll(indexBitLength);

        for (int i = 0; i < numberOfEntries; i++) {
            int entry = entries[i];
            int bucket = decodeBucketIndex(entry);
            int zeros = decodeBucketValue(entry);

            result.insert(bucket, zeros + 1); // + 1 because HLL stores leading number of zeros + 1
        }

        return result;
    }

    @Override
    public long cardinality()
    {
        // Estimate the cardinality using linear counting over the theoretical 2^indexBitLength buckets available.
        // This produces much better precision while in the sparse regime.
        int totalBuckets = numberOfBuckets(indexBitLength);
        int zeroBuckets = totalBuckets - numberOfEntries;

        return Math.round(linearCounting(zeroBuckets, totalBuckets));
    }

    @Override
    public int estimatedInMemorySize()
    {
        return SPARSE_INSTANCE_SIZE + toIntExact(sizeOf(entries));
    }

    @Override
    public int getIndexBitLength()
    {
        return indexBitLength;
    }

    /**
     * Returns a index of the entry if found. Otherwise, it returns -(insertionPoint + 1)
     */
    private int searchBucket(int bucketIndex)
    {
        int low = 0;
        int high = numberOfEntries - 1;

        while (low <= high) {
            int middle = (low + high) >>> 1;

            int middleBucketIndex = decodeBucketIndex(entries[middle]);

            if (bucketIndex > middleBucketIndex) {
                low = middle + 1;
            }
            else if (bucketIndex < middleBucketIndex) {
                high = middle - 1;
            }
            else {
                return middle;
            }
        }

        return -(low + 1); // not found... return insertion point
    }

    private int[] mergeEntries(SparseHll other)
    {
        int[] result = new int[numberOfEntries + other.numberOfEntries];
        int leftIndex = 0;
        int rightIndex = 0;

        int index = 0;
        while (leftIndex < numberOfEntries && rightIndex < other.numberOfEntries) {
            int left = decodeBucketIndex(entries[leftIndex]);
            int right = decodeBucketIndex(other.entries[rightIndex]);

            if (left < right) {
                result[index++] = entries[leftIndex++];
            }
            else if (left > right) {
                result[index++] = other.entries[rightIndex++];
            }
            else {
                int value = Math.max(decodeBucketValue(entries[leftIndex]), decodeBucketValue(other.entries[rightIndex]));
                result[index++] = encode(left, value);
                leftIndex++;
                rightIndex++;
            }
        }

        while (leftIndex < numberOfEntries) {
            result[index++] = entries[leftIndex++];
        }

        while (rightIndex < other.numberOfEntries) {
            result[index++] = other.entries[rightIndex++];
        }

        return Arrays.copyOf(result, index);
    }

    public Slice serialize()
    {
        int size = SizeOf.SIZE_OF_BYTE + // format tag
                SizeOf.SIZE_OF_BYTE + // p
                SizeOf.SIZE_OF_SHORT + // number of entries
                SizeOf.SIZE_OF_INT * numberOfEntries;

        DynamicSliceOutput out = new DynamicSliceOutput(size)
                .appendByte(Format.SPARSE_V2.getTag())
                .appendByte(indexBitLength)
                .appendShort(numberOfEntries);

        for (int i = 0; i < numberOfEntries; i++) {
            out.appendInt(entries[i]);
        }

        return out.slice();
    }

    @Override
    public int estimatedSerializedSize()
    {
        return SizeOf.SIZE_OF_SHORT // type + version
                + SizeOf.SIZE_OF_BYTE  // p
                + SizeOf.SIZE_OF_SHORT // numberOfEntries
                + SizeOf.SIZE_OF_INT * numberOfEntries; // entries
    }

    private static void validatePrefixLength(int indexBitLength)
    {
        checkArgument(indexBitLength >= 1 && indexBitLength <= 16, "indexBitLength is out of range");
    }

    @VisibleForTesting
    public void verify()
    {
        checkState(numberOfEntries <= entries.length,
                "Expected number of hashes (%s) larger than array length (%s)",
                numberOfEntries, entries.length);

        checkState(Ordering.from(comparingInt(e -> decodeBucketIndex((Integer) e)))
                        .isOrdered(Ints.asList(Arrays.copyOf(entries, numberOfEntries))),
                "entries are not sorted");
    }
}
