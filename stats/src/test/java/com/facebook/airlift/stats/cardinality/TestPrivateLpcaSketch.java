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

import com.facebook.slice.Slice;
import org.testng.annotations.Test;

import static com.facebook.slice.testing.SliceAssertions.assertSlicesEqual;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestPrivateLpcaSketch
{
    @Test
    public void testThresholding()
    {
        HyperLogLog hll = HyperLogLog.newInstance(1024);
        for (int i = 0; i < 100_000; i++) {
            hll.add(i);
        }
        PrivateLpcaSketch lpca = new PrivateLpcaSketch(hll, 1.0, 1.0, new TestingRandomizationStrategy());
        int threshold = lpca.getThreshold();
        int[] rawBuckets = getBucketValues(hll);
        for (int i = 0; i < rawBuckets.length; i++) {
            assertEquals(getBit(lpca, i), rawBuckets[i] > threshold);
        }
    }

    @Test
    public void testRoundTrip()
    {
        HyperLogLog hll = HyperLogLog.newInstance(1024);
        for (int i = 0; i < 100_000; i++) {
            hll.add(i);
        }
        PrivateLpcaSketch one = new PrivateLpcaSketch(hll, 1.0, 1.0);
        Slice serialized = one.serialize();
        PrivateLpcaSketch two = new PrivateLpcaSketch(serialized);
        Slice reserialized = two.serialize();
        assertSlicesEqual(serialized, reserialized);
    }

    @Test
    public void testBitmapSize()
    {
        // The bitmap should consist of 1 bit per bucket (1 byte per 8 buckets).
        int[] bucketCounts = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096};
        for (int count : bucketCounts) {
            HyperLogLog hll = HyperLogLog.newInstance(count);
            PrivateLpcaSketch lpca = new PrivateLpcaSketch(hll, 1.0, 1.0);
            assertEquals(lpca.getBitmap().length * 8, count);
        }
    }

    @Test
    public void testUpdate()
    {
        HyperLogLog hll1 = HyperLogLog.newInstance(1024);
        HyperLogLog hll2 = HyperLogLog.newInstance(1024);
        for (int i = 0; i < 100_000; i++) {
            hll1.add(i + 1);
            hll2.add(-i);
        }

        PrivateLpcaSketch lpca = new PrivateLpcaSketch(hll1, 1.0, 1.0, new TestingRandomizationStrategy());
        lpca.update(hll2);

        int threshold = lpca.getThreshold();
        int[] values1 = getBucketValues(hll1);
        int[] values2 = getBucketValues(hll2);
        for (int i = 0; i < values1.length; i++) {
            assertEquals(getBit(lpca, i), Math.max(values1[i], values2[i]) > threshold);
        }
    }

    @Test
    public void testUpdateIncompatible()
    {
        HyperLogLog hll1 = HyperLogLog.newInstance(1024);
        HyperLogLog hll2 = HyperLogLog.newInstance(512);
        PrivateLpcaSketch lpca = new PrivateLpcaSketch(hll1, 1.0, 1.0, new TestingRandomizationStrategy());

        boolean thrown = false;

        try {
            lpca.update(hll2); // should throw IllegalArgumentException
        }
        catch (IllegalArgumentException e) {
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void testSetBit()
    {
        HyperLogLog hll = HyperLogLog.newInstance(32);
        PrivateLpcaSketch lpca = new PrivateLpcaSketch(hll, 1.0, 1.0, new TestingRandomizationStrategy());

        for (int b = 0; b < lpca.getNumberOfBuckets(); b++) {
            lpca.setBit(b, true);
            assertTrue(getBit(lpca, b));
            lpca.setBit(b, false);
            assertFalse(getBit(lpca, b));
        }
    }

    @Test
    public void testFlipBit()
    {
        HyperLogLog hll = HyperLogLog.newInstance(32);
        PrivateLpcaSketch lpca = new PrivateLpcaSketch(hll, 1.0, 1.0, new TestingRandomizationStrategy());

        lpca.setBit(10, true);
        assertTrue(getBit(lpca, 10));
        lpca.flipBit(10);
        assertFalse(getBit(lpca, 10));
        lpca.flipBit(10);
        assertTrue(getBit(lpca, 10));
    }

    @Test
    public void testBitProportion()
    {
        HyperLogLog hll = HyperLogLog.newInstance(32);
        PrivateLpcaSketch lpca = new PrivateLpcaSketch(hll, 1.0, 1.0, new TestingRandomizationStrategy());

        int cutoff = 18;
        for (int i = 0; i < lpca.getNumberOfBuckets(); i++) {
            lpca.setBit(i, i < cutoff);
        }

        assertEquals(lpca.getRawBitProportion(), 18.0 / 32.0);
    }

    private boolean getBit(PrivateLpcaSketch lpca, int bucket)
    {
        int b = PrivateLpcaSketch.bitmapByteIndex(bucket);
        int shift = PrivateLpcaSketch.bitmapBitShift(bucket);

        return ((lpca.getBitmap()[b] >> shift) & 1) == 1;
    }

    private int[] getBucketValues(HyperLogLog hll)
    {
        int[] values = new int[hll.getNumberOfBuckets()];
        hll.eachBucket((i, value) -> values[i] = value);
        return values;
    }
}
