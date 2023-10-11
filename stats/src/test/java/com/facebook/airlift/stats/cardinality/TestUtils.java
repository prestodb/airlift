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

import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public final class TestUtils
{
    private TestUtils() {}

    public static List<Long> sequence(int start, int end)
    {
        ImmutableList.Builder<Long> builder = ImmutableList.builder();

        for (long i = start; i < end; i++) {
            builder.add(i);
        }

        return builder.build();
    }

    public static long createHashForBucket(int indexBitLength, int bucket, int leadingZeros)
    {
        // put a 1 in the indexBitLength + i + 1-th place
        long hash = 1L << (Long.SIZE - (indexBitLength + leadingZeros + 1));
        // set index bits to corresponding bucket index
        hash |= (long) bucket << (Long.SIZE - indexBitLength);
        return hash;
    }

    @Test
    public void testPowerOf2()
    {
        for (int i = 1; i < 20; i++) {
            assertTrue(Utils.isPowerOf2(Math.round(Math.pow(2, i))));
            assertFalse(Utils.isPowerOf2(Math.round(Math.pow(2, i)) + 1));
        }
    }

    @Test
    public void testNumberOfBuckets()
    {
        for (int i = 1; i < 20; i++) {
            assertEquals(Utils.numberOfBuckets(i), Math.round(Math.pow(2, i)));
        }
    }

    @Test
    public void testIndexBitLength()
    {
        for (int i = 1; i < 20; i++) {
            assertEquals(Utils.indexBitLength((int) Math.pow(2, i)), i);
        }
    }

    @Test
    public void testNumberOfLeadingZeros()
    {
        for (int indexBitLength : new int[]{6, 12, 18}) {
            for (int i = 0; i < Long.SIZE - indexBitLength; i++) {
                long hash = createHashForBucket(indexBitLength, 0, i);
                assertEquals(Utils.numberOfLeadingZeros(hash, indexBitLength), i);
            }
        }
    }

    @Test
    public void testNumberOfTrailingZeros()
    {
        for (int indexBitLength : new int[]{6, 12, 18}) {
            for (int i = 0; i < Long.SIZE - 1; i++) {
                long hash = 1L << i;
                assertEquals(Utils.numberOfTrailingZeros(hash, indexBitLength), Math.min(i, Long.SIZE - indexBitLength));
            }
        }
    }

    @Test
    public void testComputeIndex()
    {
        for (int indexBitLength : new int[]{6, 12, 18}) {
            long index = 5L;
            long hash = index << (Long.SIZE - indexBitLength);
            assertEquals(Utils.computeIndex(hash, indexBitLength), index);
        }
    }
}
