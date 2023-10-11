package com.facebook.airlift.stats.cardinality;

import io.airlift.slice.Slice;
import org.openjdk.jol.info.ClassLayout;
import org.testng.annotations.Test;

import static io.airlift.slice.testing.SliceAssertions.assertSlicesEqual;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class TestSfmSketch
{
    @Test
    public void testRoundTrip()
    {
        SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
        for (int i = 0; i < 100_000; i++) {
            sketch.add(i);
        }
        sketch.enablePrivacy(2);
        Slice serialized = sketch.serialize();
        SfmSketch unserialized = SfmSketch.deserialize(serialized, new TestingSeededRandomizationStrategy(1));
        assertSlicesEqual(serialized, unserialized.serialize());
    }

    @Test
    public void testPrivacyEnabled()
    {
        SfmSketch sketch = SfmSketch.create(32, 24, new TestingSeededRandomizationStrategy(1));
        assertFalse(sketch.isPrivacyEnabled());
        sketch.enablePrivacy(SfmSketch.NON_PRIVATE_EPSILON);
        assertFalse(sketch.isPrivacyEnabled());
        sketch.enablePrivacy(1.23);
        assertTrue(sketch.isPrivacyEnabled());
    }

    @Test
    public void testSerializedSize()
    {
        SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
        sketch.enablePrivacy(1.23);
        assertEquals(sketch.estimatedSerializedSize(), sketch.serialize().length());
    }

    @Test
    public void testRetainedSize()
    {
        SfmSketch sketch = SfmSketch.create(4096, 24, new SecureRandomizationStrategy());
        sketch.enablePrivacy(4);
        assertEquals(sketch.getRetainedSizeInBytes(),
                ClassLayout.parseClass(SfmSketch.class).instanceSize() +
                        ClassLayout.parseClass(SecureRandomizationStrategy.class).instanceSize() +
                        sketch.getBitmap().getRetainedSizeInBytes());
    }

    @Test
    public void testBitmapSize()
    {
        int[] buckets = {32, 64, 512, 1024, 4096, 32768};
        int[] precisions = {8, 16, 24, 32};

        for (int numberOfBuckets : buckets) {
            for (int precision : precisions) {
                SfmSketch sketch = SfmSketch.create(numberOfBuckets, precision, new TestingSeededRandomizationStrategy(1));
                assertEquals(sketch.getBitmap().length(), numberOfBuckets * precision);
            }
        }
    }

    @Test
    public void testMergeNonPrivate()
    {
        SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
        SfmSketch sketch2 = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));

        // insert 100,000 non-negative integers + 100,000 negative integers
        for (int i = 0; i < 100_000; i++) {
            sketch.add(i);
            sketch2.add(-i - 1);
        }
        Bitmap before = sketch.getBitmap().clone();
        sketch.mergeWith(sketch2);

        // The two bitmaps should be merged with OR,
        // and the resulting bitmap is not private.
        assertEquals(sketch.getBitmap().toBytes(), before.or(sketch2.getBitmap()).toBytes());
        assertFalse(sketch.isPrivacyEnabled());
    }

    @Test
    public void testMergePrivate()
    {
        SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
        SfmSketch sketch2 = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(2));

        // insert 100,000 non-negative integers + 100,000 negative integers
        for (int i = 0; i < 100_000; i++) {
            sketch.add(i);
            sketch2.add(-i - 1);
        }

        Bitmap nonPrivateBitmap = sketch.getBitmap().clone();
        Bitmap nonPrivateBitmap2 = sketch2.getBitmap().clone();

        sketch.enablePrivacy(3);
        sketch2.enablePrivacy(4);
        double p1 = sketch.getRandomizedResponseProbability();
        double p2 = sketch2.getRandomizedResponseProbability();

        Bitmap unmergedBitmap = sketch.getBitmap().clone();
        sketch.mergeWith(sketch2);

        // The resulting bitmap is a randomized merge equivalent to a noisy (not deterministic) OR.
        // As a result, the bitmap should not equal an OR, but it should have roughly the same number
        // of 1-bits as an OR that is flipped with the merged randomizedResponseProbability.
        // The resulting merged sketch is private.
        assertTrue(sketch.isPrivacyEnabled());
        assertEquals(sketch.getRandomizedResponseProbability(), SfmSketch.mergeRandomizedResponseProbabilities(p1, p2));
        assertNotEquals(sketch.getBitmap().toBytes(), unmergedBitmap.or(sketch2.getBitmap()).toBytes());

        int actualBitCount = sketch.getBitmap().getBitCount();
        Bitmap hypotheticalBitmap = nonPrivateBitmap.or(nonPrivateBitmap2);
        hypotheticalBitmap.flipAll(sketch.getRandomizedResponseProbability(), new TestingSeededRandomizationStrategy(1));
        // The number of 1-bits in the merged sketch should approximately equal the number of 1-bits in our hypothetical bitmap.
        assertEquals(hypotheticalBitmap.getBitCount(), actualBitCount, 100);
    }

    @Test
    public void testMergeMixed()
    {
        SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
        SfmSketch sketch2 = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(2));
        for (int i = 0; i < 100_000; i++) {
            sketch.add(i);
            sketch2.add(-i - 1);
        }
        sketch2.enablePrivacy(3);
        Bitmap before = sketch.getBitmap().clone();
        sketch.mergeWith(sketch2);

        // The resulting sketch is private.
        assertTrue(sketch.isPrivacyEnabled());

        // A mixed-privacy merge is mathematically similar to a normal private merge, but
        // it turns out that some bits are deterministic. In particular, the bits of the
        // merged sketch corresponding to 0s in the non-private sketch should exactly match
        // the private sketch.
        for (int i = 0; i < before.length(); i++) {
            if (!before.getBit(i)) {
                assertEquals(sketch.getBitmap().getBit(i), sketch2.getBitmap().getBit(i));
            }
        }
    }

    @Test
    public void testMergedProbabilities()
    {
        // should be symmetric
        assertEquals(SfmSketch.mergeRandomizedResponseProbabilities(0.1, 0.2), SfmSketch.mergeRandomizedResponseProbabilities(0.2, 0.1));

        // private + nonprivate = private
        assertEquals(SfmSketch.mergeRandomizedResponseProbabilities(0, 0.1), 0.1);
        assertEquals(SfmSketch.mergeRandomizedResponseProbabilities(0.15, 0), 0.15);

        // nonprivate + nonprivate = nonprivate
        assertEquals(SfmSketch.mergeRandomizedResponseProbabilities(0.0, 0.0), 0.0);

        // private + private = private (noisier)
        // In particular, according to https://arxiv.org/pdf/2302.02056.pdf, Theorem 4.8, two sketches
        // with epsilon1 and epsilon2 should have a merged epsilonStar of:
        // -log(e^-epsilon1 + e^-epsilon2 - e^-(epsilon1 + epsilon2))
        double epsilon1 = 1.2;
        double epsilon2 = 3.4;
        double p1 = SfmSketch.getRandomizedResponseProbability(epsilon1);
        double p2 = SfmSketch.getRandomizedResponseProbability(epsilon2);
        double epsilonStar = -Math.log(Math.exp(-epsilon1) + Math.exp(-epsilon2) - Math.exp(-(epsilon1 + epsilon2)));
        double pStar = SfmSketch.getRandomizedResponseProbability(epsilonStar);
        assertEquals(SfmSketch.mergeRandomizedResponseProbabilities(p1, p2), pStar, 1E-6);
        // note: the merged sketch is noisier (higher probability of flipped bits)
        assertTrue(pStar > Math.max(p1, p2));
    }

    @Test
    public void testEmptySketchCardinality()
    {
        SfmSketch nonPrivateSketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
        SfmSketch privateSketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(2));
        privateSketch.enablePrivacy(3);

        // Non-private should return exactly 0
        assertEquals(nonPrivateSketch.cardinality(), 0);

        // Private will be a noisy sketch, so it should return approximately zero, but will be rather noisy.
        assertEquals(privateSketch.cardinality(), 0, 200);
    }

    @Test
    public void testSmallCardinality()
    {
        int[] ns = {1, 5, 10, 50, 100, 200, 500, 1000};

        for (int n : ns) {
            SfmSketch nonPrivateSketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
            SfmSketch privateSketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(2));

            for (int i = 0; i < n; i++) {
                nonPrivateSketch.add(i);
                privateSketch.add(i);
            }

            privateSketch.enablePrivacy(3);

            // Non-private should actually be quite good for small numbers
            assertEquals(nonPrivateSketch.cardinality(), n, Math.max(10, 0.1 * n));

            // Private isn't quite as good...
            assertEquals(privateSketch.cardinality(), n, 200);
        }
    }

    @Test
    public void testActualCardinalityEstimates()
    {
        // Since we don't have precise accuracy guarantees, this test is pretty loose.
        // Note: this is slow for cardinalities beyond, say, 1 million. See `testSimulatedCardinalityEstimates` below.
        int[] magnitudes = {4, 5, 6};
        double[] epsilons = {4, SfmSketch.NON_PRIVATE_EPSILON};
        for (int mag : magnitudes) {
            int n = (int) Math.pow(10, mag);
            for (double eps : epsilons) {
                SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
                for (int i = 0; i < n; i++) {
                    sketch.add(i);
                }
                sketch.enablePrivacy(eps);
                assertEquals(sketch.cardinality(), n, n * 0.1);
            }
        }
    }

    @Test
    public void testSimulatedCardinalityEstimates()
    {
        // Instead of creating sketches by adding items, we simulate them for fast testing of huge cardinalities.
        // For reference, 10^33 is one decillion.
        // The goal here is to test general functionality and numerical stability.
        int[] magnitudes = {6, 9, 12, 15, 18, 21, 24, 27, 30, 33};
        double[] epsilons = {4, SfmSketch.NON_PRIVATE_EPSILON};
        for (int mag : magnitudes) {
            int n = (int) Math.pow(10, mag);
            for (double eps : epsilons) {
                SfmSketch sketch = createSketchWithTargetCardinality(4096, 24, eps, n);
                assertEquals(sketch.cardinality(), n, n * 0.1);
            }
        }
    }

    @Test
    public void testMergedCardinalities()
    {
        double[] epsilons = {3, 4, SfmSketch.NON_PRIVATE_EPSILON};

        // Test each pair of epsilons
        // This gives us equal private epsilons, unequal private epsilons, mixed private and nonprivate, and totally nonprivate
        for (double eps1 : epsilons) {
            for (double eps2 : epsilons) {
                SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
                SfmSketch sketch2 = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(2));
                // insert 300,000 positive integers and 200,000 negative integers
                for (int i = 0; i < 300_000; i++) {
                    sketch.add(i + 1);
                    if (i < 200_000) {
                        sketch2.add(-i);
                    }
                }

                sketch.enablePrivacy(eps1);
                sketch2.enablePrivacy(eps2);
                sketch.mergeWith(sketch2);
                assertEquals(sketch.cardinality(), 500_000, 500_000 * 0.1);
            }
        }
    }

    @Test
    public static void testEnablePrivacy()
    {
        SfmSketch sketch = SfmSketch.create(4096, 24, new TestingSeededRandomizationStrategy(1));
        double epsilon = 4;

        for (int i = 0; i < 100_000; i++) {
            sketch.add(i);
        }

        long cardinalityBefore = sketch.cardinality();
        sketch.enablePrivacy(epsilon);
        long cardinalityAfter = sketch.cardinality();

        // Randomized response probability should reflect the new (private) epsilon
        assertEquals(sketch.getRandomizedResponseProbability(), SfmSketch.getRandomizedResponseProbability(epsilon));
        assertTrue(sketch.isPrivacyEnabled());

        // Cardinality should remain approximately the same
        assertEquals(cardinalityAfter, cardinalityBefore, cardinalityBefore * 0.1);
    }

    private static SfmSketch createSketchWithTargetCardinality(int numberOfBuckets, int precision, double epsilon, int cardinality)
    {
        // Building a sketch by adding items is really slow (O(n)) if you want to test billions/trillions/quadrillions/etc.
        // Simulating the sketch is much faster (O(buckets * precision)).
        RandomizationStrategy random = new TestingSeededRandomizationStrategy(1);
        SfmSketch sketch = SfmSketch.create(numberOfBuckets, precision, random);
        Bitmap bitmap = sketch.getBitmap();
        double c1 = sketch.getOnProbability();
        double c2 = sketch.getOnProbability() - sketch.getRandomizedResponseProbability();

        for (int l = 0; l < precision; l++) {
            double p = c1 - c2 * Math.pow(1 - Math.pow(2, -(l + 1)) / numberOfBuckets, cardinality);
            for (int b = 0; b < numberOfBuckets; b++) {
                bitmap.setBit(sketch.getBitLocation(b, l), random.nextBoolean(p));
            }
        }

        sketch.enablePrivacy(epsilon);
        return sketch;
    }
}
