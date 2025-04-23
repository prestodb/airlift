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
package com.facebook.drift.codec.internal.builtin;

import com.facebook.drift.codec.ThriftCodec;
import com.facebook.drift.codec.metadata.ThriftType;
import com.facebook.drift.protocol.TProtocolReader;
import com.facebook.drift.protocol.TProtocolWriter;

import javax.annotation.concurrent.Immutable;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

@Immutable
public class ByteBufferThriftCodec
        implements ThriftCodec<ByteBuffer>
{
    @Override
    public ThriftType getType()
    {
        return ThriftType.BINARY;
    }

    @Override
    public ByteBuffer read(TProtocolReader protocol)
            throws Exception
    {
        requireNonNull(protocol, "protocol is null");
        return protocol.readBinary();
    }

    @Override
    public void write(ByteBuffer value, TProtocolWriter protocol)
            throws Exception
    {
        requireNonNull(value, "value is null");
        requireNonNull(protocol, "protocol is null");
        protocol.writeBinary(value);
    }
}
