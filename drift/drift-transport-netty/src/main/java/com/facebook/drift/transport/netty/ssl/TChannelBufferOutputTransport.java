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
package com.facebook.drift.transport.netty.ssl;

import com.facebook.airlift.concurrent.NotThreadSafe;
import com.facebook.drift.protocol.TTransport;
import com.google.errorprone.annotations.CheckReturnValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCounted;

@NotThreadSafe
public class TChannelBufferOutputTransport
        implements TTransport, ReferenceCounted
{
    private final ByteBuf buffer;

    public TChannelBufferOutputTransport(ByteBufAllocator byteBufAllocator)
    {
        this(byteBufAllocator.buffer(1024));
    }

    public TChannelBufferOutputTransport(ByteBuf buffer)
    {
        this.buffer = buffer;
    }

    public ByteBuf getBuffer()
    {
        return buffer.retainedDuplicate();
    }

    @Override
    public void write(byte[] buf, int off, int len)
    {
        buffer.writeBytes(buf, off, len);
    }

    @Override
    public void read(byte[] buf, int off, int len)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int refCnt()
    {
        return buffer.refCnt();
    }

    @Override
    public ReferenceCounted retain()
    {
        buffer.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment)
    {
        buffer.retain(increment);
        return this;
    }

    @Override
    public ReferenceCounted touch()
    {
        buffer.touch();
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint)
    {
        buffer.touch(hint);
        return this;
    }

    @CheckReturnValue
    @Override
    public boolean release()
    {
        return buffer.release();
    }

    @Override
    public boolean release(int decrement)
    {
        return buffer.release(decrement);
    }
}
