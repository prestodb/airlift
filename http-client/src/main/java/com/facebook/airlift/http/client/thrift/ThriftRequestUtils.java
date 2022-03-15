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
package com.facebook.airlift.http.client.thrift;

import com.facebook.airlift.http.client.Request;
import com.facebook.drift.codec.ThriftCodec;
import com.facebook.drift.transport.netty.codec.Protocol;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.facebook.airlift.http.client.Request.Builder.prepareDelete;
import static com.facebook.airlift.http.client.Request.Builder.prepareGet;
import static com.facebook.airlift.http.client.Request.Builder.preparePost;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

public class ThriftRequestUtils
{
    public static final String APPLICATION_THRIFT_BINARY = "application/x-thrift+binary";
    public static final String APPLICATION_THRIFT_COMPACT = "application/x-thrift+compact";
    public static final String APPLICATION_THRIFT_FB_COMPACT = "application/x-thrift+fb_compact";
    public static final List<String> validThriftMimeTypes = ImmutableList.of(APPLICATION_THRIFT_BINARY, APPLICATION_THRIFT_COMPACT, APPLICATION_THRIFT_FB_COMPACT);

    private ThriftRequestUtils() {}

    public static Request.Builder prepareThriftPost(Protocol protocol)
    {
        String type = getType(protocol);
        return preparePost()
                .setHeader(ACCEPT, type)
                .setHeader(CONTENT_TYPE, type);
    }

    public static <T> Request.Builder prepareThriftPost(Protocol protocol, T instance, ThriftCodec<T> thriftCodec)
    {
        return prepareThriftPost(protocol)
                .setBodyGenerator(createThriftBodyGenerator(instance, thriftCodec, protocol));
    }

    public static Request.Builder prepareThriftGet(Protocol protocol)
    {
        String type = getType(protocol);
        return prepareGet()
                .setHeader(ACCEPT, type);
    }

    public static Request.Builder prepareThriftDelete(Protocol protocol)
    {
        String type = getType(protocol);
        return prepareDelete()
                .setHeader(ACCEPT, type);
    }

    private static <T> ThriftBodyGenerator<T> createThriftBodyGenerator(T instance, ThriftCodec<T> thriftCodec, Protocol protocol)
    {
        return new ThriftBodyGenerator<>(instance, thriftCodec, protocol);
    }

    private static String getType(Protocol protocol)
    {
        switch (protocol) {
            case BINARY:
                return APPLICATION_THRIFT_BINARY;
            case COMPACT:
                return APPLICATION_THRIFT_COMPACT;
            case FB_COMPACT:
                return APPLICATION_THRIFT_FB_COMPACT;
            default:
                throw new IllegalArgumentException("Invalid thrift protocol");
        }
    }
}
