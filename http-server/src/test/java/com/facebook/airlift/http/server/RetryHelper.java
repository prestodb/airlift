/*
 * Copyright 2010 Proofpoint, Inc.
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
package com.facebook.airlift.http.server;

import com.facebook.airlift.http.client.HttpClient;
import com.facebook.airlift.http.client.Request;
import com.facebook.airlift.http.client.ResponseHandler;
import com.facebook.airlift.log.Logger;

import java.io.EOFException;
import java.io.UncheckedIOException;

public final class RetryHelper
{
    private static final Logger log = Logger.get(RetryHelper.class);
    private RetryHelper() {}

    public static <T> T executeWithRetry(HttpClient client, Request request, ResponseHandler<T, RuntimeException> handler)
    {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.execute(request, handler);
            }
            catch (UncheckedIOException e) {
                log.warn("TODO: Known race condition with Jetty 12.x which sometimes results in a UncheckedIOException. Need to reliably reproduce and file a bug on Jetty");
                if (!isRetryable(e) || attempt == maxAttempts) {
                    throw e;
                }
                try {
                    Thread.sleep(5);
                }
                catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private static boolean isRetryable(UncheckedIOException e)
    {
        Throwable cause = e.getCause();
        return cause instanceof EOFException;
    }
}
