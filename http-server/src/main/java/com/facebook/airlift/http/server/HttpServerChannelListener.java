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
package com.facebook.airlift.http.server;

import jakarta.annotation.Nullable;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.EventsHandler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class HttpServerChannelListener
        extends EventsHandler
{
    private static final String REQUEST_BEGIN_ATTRIBUTE = HttpServerChannelListener.class.getName() + ".begin";
    private static final String REQUEST_BEGIN_TO_DISPATCH_ATTRIBUTE = HttpServerChannelListener.class.getName() + ".begin_to_dispatch";
    private static final String REQUEST_BEGIN_TO_END_ATTRIBUTE = HttpServerChannelListener.class.getName() + ".begin_to_end";
    private static final String RESPONSE_CONTENT_TIMESTAMPS_ATTRIBUTE = HttpServerChannelListener.class.getName() + ".response_content_timestamps";

    private final DelimitedRequestLog logger;

    public HttpServerChannelListener(DelimitedRequestLog logger)
    {
        this.logger = requireNonNull(logger, "logger is null");
    }

    @Override
    protected void onBeforeHandling(Request request)
    {
        request.setAttribute(REQUEST_BEGIN_ATTRIBUTE, System.nanoTime());
    }

    @Override
    protected void onAfterHandling(Request request, boolean handled, Throwable failure)
    {
        long requestBeginTime = (Long) request.getAttribute(REQUEST_BEGIN_ATTRIBUTE);
        request.setAttribute(REQUEST_BEGIN_TO_END_ATTRIBUTE, System.nanoTime() - requestBeginTime);
    }

    @Override
    protected void onResponseBegin(Request request, int status, HttpFields headers)
    {
        if (request.getAttribute(REQUEST_BEGIN_TO_END_ATTRIBUTE) == null) {
            this.onComplete(request, status, headers, null);
        }
        request.setAttribute(RESPONSE_CONTENT_TIMESTAMPS_ATTRIBUTE, new ArrayList<>());
    }

    @Override
    protected void onResponseWrite(Request request, boolean last, ByteBuffer content)
    {
        List<Long> contentTimestamps = (List<Long>) request.getAttribute(RESPONSE_CONTENT_TIMESTAMPS_ATTRIBUTE);
        contentTimestamps.add(System.nanoTime());
    }

    @Override
    protected void onComplete(Request request, Throwable failure)
    {
        onCompleteHelper(request, -1, HttpFields.EMPTY, failure);
    }

    @Override
    protected void onComplete(Request request, int status, HttpFields headers, Throwable failure)
    {
        onCompleteHelper(request, status, headers, failure);
    }

    private void onCompleteHelper(Request request, int status, HttpFields headers, Throwable failure)
    {
        List<Long> contentTimestamps = (List<Long>) request.getAttribute(RESPONSE_CONTENT_TIMESTAMPS_ATTRIBUTE);
        long firstToLastContentTimeInMillis = -1;
        if (contentTimestamps.size() > 0) {
            firstToLastContentTimeInMillis = NANOSECONDS.toMillis(contentTimestamps.get(contentTimestamps.size() - 1) - contentTimestamps.get(0));
        }
        long beginToDispatchMillis = NANOSECONDS.toMillis((Long) request.getAttribute(REQUEST_BEGIN_TO_DISPATCH_ATTRIBUTE));
        long beginToEndMillis = NANOSECONDS.toMillis((Long) request.getAttribute(REQUEST_BEGIN_TO_END_ATTRIBUTE));
        logger.log(request,
                status,
                headers,
                beginToDispatchMillis,
                beginToEndMillis,
                firstToLastContentTimeInMillis,
                processContentTimestamps(contentTimestamps));
    }

    /**
     * Calculate the summary statistics for the interarrival time of the onResponseContent callbacks.
     */
    @Nullable
    private static DoubleSummaryStats processContentTimestamps(List<Long> contentTimestamps)
    {
        requireNonNull(contentTimestamps, "contentTimestamps is null");

        // no content (HTTP 204) or there was a single response chunk (so no interarrival time)
        if (contentTimestamps.size() == 0 || contentTimestamps.size() == 1) {
            return null;
        }

        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        long previousTimestamp = contentTimestamps.get(0);
        for (int i = 1; i < contentTimestamps.size(); i++) {
            long timestamp = contentTimestamps.get(i);
            statistics.accept(NANOSECONDS.toMillis(timestamp - previousTimestamp));
            previousTimestamp = timestamp;
        }
        return new DoubleSummaryStats(statistics);
    }
}
