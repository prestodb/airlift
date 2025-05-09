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
package com.facebook.drift.integration.guice;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import static java.lang.Math.toIntExact;
import static java.util.Objects.requireNonNull;

public class ThrowingServiceHandler
        implements ThrowingService
{
    @GuardedBy("this")
    private SettableFuture<String> awaitFuture;

    @Override
    public void fail(String message, boolean retryable)
            throws ExampleException
    {
        throw new ExampleException(message, retryable);
    }

    @Override
    public byte[] generateTooLargeFrame()
    {
        return new byte[toIntExact(MAX_FRAME_SIZE.toBytes()) + 1];
    }

    @Override
    public String acceptBytes(byte[] bytes)
    {
        return "OK";
    }

    @Override
    public synchronized ListenableFuture<String> await()
    {
        if (awaitFuture == null) {
            awaitFuture = SettableFuture.create();
        }
        return awaitFuture;
    }

    @Override
    public synchronized String release()
    {
        requireNonNull(awaitFuture, "release when there is no await is called");
        awaitFuture.set("OK");
        awaitFuture = null;
        return "OK";
    }
}
