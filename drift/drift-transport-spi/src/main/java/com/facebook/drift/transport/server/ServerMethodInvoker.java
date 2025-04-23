/*
 * Copyright (C) 2013 Facebook, Inc.
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
package com.facebook.drift.transport.server;

import com.facebook.drift.TApplicationException;
import com.facebook.drift.transport.MethodMetadata;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Optional;

public interface ServerMethodInvoker
{
    /**
     * @return metadata for the specified method or null if method is not supported
     */
    Optional<MethodMetadata> getMethodMetadata(String name);

    /**
     * Invoke the specified method asynchronously.
     * <p>
     * If the invocation fails, the future should contain a known application exception or a raw
     * {@link TApplicationException}.
     */
    ListenableFuture<Object> invoke(ServerInvokeRequest request);

    /**
     * Record results timing for a method invocation.
     */
    void recordResult(String methodName, long startTime, ListenableFuture<Object> result);
}
