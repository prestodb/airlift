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

/**
 * A deterministic alternative to randomness (i.e., mock random numbers)
 */
public class TestingRandomizationStrategy
        implements RandomizationStrategy
{
    public double effectiveProbability(double probability)
    {
        return 0.0;
    }

    public boolean nextBoolean(double probability)
    {
        return false;
    }

    public double nextLaplace(double scale)
    {
        return 0.0;
    }
}
