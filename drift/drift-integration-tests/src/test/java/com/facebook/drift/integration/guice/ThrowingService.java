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

import com.facebook.airlift.units.DataSize;
import com.facebook.drift.TException;
import com.facebook.drift.annotations.ThriftMethod;
import com.facebook.drift.annotations.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;

import static com.facebook.airlift.units.DataSize.Unit.KILOBYTE;

@ThriftService("throwing")
public interface ThrowingService
{
    DataSize MAX_FRAME_SIZE = new DataSize(10, KILOBYTE);

    @ThriftMethod
    void fail(String message, boolean retryable)
            throws ExampleException;

    @ThriftMethod
    byte[] generateTooLargeFrame()
            throws TException;

    @ThriftMethod
    String acceptBytes(byte[] bytes)
            throws TException;

    @ThriftMethod
    ListenableFuture<String> await();

    @ThriftMethod
    String release();
}
