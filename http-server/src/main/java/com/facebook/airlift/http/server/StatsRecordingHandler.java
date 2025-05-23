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

import com.facebook.airlift.units.Duration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.jetty.server.Request.getContentBytesRead;
import static org.eclipse.jetty.server.Request.getTimeStamp;
import static org.eclipse.jetty.server.Response.getContentBytesWritten;

public class StatsRecordingHandler
        implements RequestLog
{
    private final RequestStats stats;

    public StatsRecordingHandler(RequestStats stats)
    {
        this.stats = stats;
    }

    @Override
    public void log(Request request, Response response)
    {
        Duration requestTime = new Duration(max(0, System.currentTimeMillis() - getTimeStamp(request)), MILLISECONDS);
        stats.record(getContentBytesRead(request), getContentBytesWritten(response), requestTime);
    }
}
