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
import com.google.errorprone.annotations.Immutable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Immutable
public class OptionalThriftCodec<T>
        implements ThriftCodec<Optional<T>>
{
    private final ThriftCodec<T> elementCodec;
    private final ThriftType type;

    public OptionalThriftCodec(ThriftType type, ThriftCodec<T> elementCodec)
    {
        this.type = requireNonNull(type, "type is null");
        this.elementCodec = requireNonNull(elementCodec, "elementCodec is null");
    }

    @Override
    public ThriftType getType()
    {
        return type;
    }

    @Override
    public Optional<T> read(TProtocolReader protocol)
            throws Exception
    {
        requireNonNull(protocol, "protocol is null");
        return Optional.of(elementCodec.read(protocol));
    }

    @Override
    public void write(Optional<T> value, TProtocolWriter protocol)
            throws Exception
    {
        requireNonNull(value, "value is null");
        requireNonNull(protocol, "protocol is null");
        // write can not be called with a missing value, and instead the write should be skipped
        // after check the result from isNull
        if (!value.isPresent()) {
            throw new IllegalArgumentException("value is not present");
        }
        elementCodec.write(value.get(), protocol);
    }

    @Override
    public boolean isNull(Optional<T> value)
    {
        return value == null || !value.isPresent();
    }
}
