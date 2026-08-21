package com.facebook.airlift.stats;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestDistribution
{
    @Test
    public void testMerge()
    {
        Distribution distribution1 = new Distribution();
        Distribution distribution2 = new Distribution();

        for (int i = 0; i < 10; i++) {
            distribution1.add(i);
        }

        for (int i = 10; i < 20; i++) {
            distribution2.add(i);
        }

        distribution1.merge(distribution2);

        assertEquals(distribution1.getP50(), 10);
        assertEquals(distribution1.getMin(), 0);
        assertEquals(distribution1.getMax(), 19);
        assertEquals(distribution1.getCount(), 20.0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Expected decayCounter to have alpha.*")
    public void testMergeWithIncompatibleAlphaFails()
    {
        Distribution distribution1 = new Distribution(0.5);
        Distribution distribution2 = new Distribution(0.7);

        distribution1.merge(distribution2);
    }
}
