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

import com.facebook.drift.codec.ThriftCodec;
import com.facebook.drift.transport.netty.codec.Protocol;

import java.io.InputStream;
import java.io.OutputStream;

public class ThriftProtocolUtils
{
    private ThriftProtocolUtils()
    {
    }

    public static <T> void write(T instance, ThriftCodec<T> thriftCodec, Protocol protocol, OutputStream output)
            throws ThriftProtocolException
    {
        try {
            thriftCodec.write(instance, protocol.createProtocol(new TOutputStreamTransport(output)));
        }
        catch (Exception e) {
            throw new ThriftProtocolException(thriftCodec.getType().getJavaType().getClass(), e);
        }
    }

    public static <T> T read(ThriftCodec<T> thriftCodec, Protocol protocol, InputStream input)
            throws ThriftProtocolException
    {
        try {
            return thriftCodec.read(protocol.createProtocol(new TInputStreamTransport(input)));
        }
        catch (Exception e) {
            throw new ThriftProtocolException(thriftCodec.getType().getJavaType().getClass(), e);
        }
    }
}
