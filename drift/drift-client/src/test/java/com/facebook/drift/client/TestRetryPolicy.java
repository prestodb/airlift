/*
 * Copyright (C) 2018 Facebook, Inc.
 *
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
package com.facebook.drift.client;

import com.facebook.drift.transport.client.DriftApplicationException;
import com.facebook.drift.transport.client.DriftClientConfig;
import org.testng.annotations.Test;

import java.util.Optional;

import static com.facebook.drift.client.ExceptionClassification.HostStatus.OVERLOADED;
import static com.facebook.drift.client.ExceptionClassification.NORMAL_EXCEPTION;
import static org.testng.Assert.assertSame;

public class TestRetryPolicy
{
    @Test
    public void testRetryUserException()
    {
        ExceptionClassification overloaded = new ExceptionClassification(Optional.of(true), OVERLOADED);
        RetryPolicy policy = new RetryPolicy(new DriftClientConfig(), classifier -> {
            if (classifier instanceof TestingUserException) {
                return overloaded;
            }
            return NORMAL_EXCEPTION;
        });
        assertSame(policy.classifyException(new DriftApplicationException(new TestingUserException()), true), overloaded);
        assertSame(policy.classifyException(new TestingUserException(), true), overloaded);
    }

    private static class TestingUserException
            extends Exception
    {}
}
