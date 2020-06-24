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

import com.facebook.airlift.http.client.BodyGenerator;
import com.facebook.drift.codec.ThriftCodec;
import com.facebook.drift.transport.netty.codec.Protocol;

import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

public class ThriftBodyGenerator<T>
        implements BodyGenerator
{
    private final T instance;
    private final ThriftCodec<T> thriftCodec;
    private final Protocol protocol;

    public ThriftBodyGenerator(T instance, ThriftCodec<T> thriftCodec, Protocol protocol)
    {
        this.instance = requireNonNull(instance, "instance is null");
        this.thriftCodec = requireNonNull(thriftCodec, "thriftCodec is null");
        this.protocol = requireNonNull(protocol, "protocol is null");
    }

    @Override
    public void write(OutputStream out)
            throws Exception
    {
        ThriftProtocolUtils.write(instance, thriftCodec, protocol, out);
    }
}
