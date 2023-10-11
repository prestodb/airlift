package com.facebook.airlift.stats.cardinality;

import java.util.Random;

/**
 * Seeded random numbers for testing
 */
public class TestingSeededRandomizationStrategy
        extends RandomizationStrategy
{
    private final Random random;

    public TestingSeededRandomizationStrategy(long seed)
    {
        this.random = new Random(seed);
    }

    public long getRetainedSizeInBytes()
    {
        return 0; // This is false, but it's not particularly important here.
    }

    public double nextDouble()
    {
        return random.nextDouble();
    }
}
