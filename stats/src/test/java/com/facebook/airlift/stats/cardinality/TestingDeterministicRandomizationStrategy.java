package com.facebook.airlift.stats.cardinality;

/**
 * Non-random numbers for testing
 */
public class TestingDeterministicRandomizationStrategy
        extends RandomizationStrategy
{
    public TestingDeterministicRandomizationStrategy() {}

    public long getRetainedSizeInBytes()
    {
        return 0; // This is false, but it's not particularly important here.
    }

    public double nextDouble()
    {
        return 0.5;
    }
}
