/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.drift.integration.scribe.drift;

import com.facebook.drift.TException;
import com.facebook.drift.annotations.ThriftHeader;
import com.facebook.drift.annotations.ThriftMethod;
import com.facebook.drift.annotations.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

@ThriftService
public interface DriftAsyncScribe
        extends AutoCloseable
{
    @ThriftMethod("Log")
    ListenableFuture<DriftResultCode> log(@ThriftHeader("header") String headerValue, List<DriftLogEntry> logEntries)
            throws TException;
}
