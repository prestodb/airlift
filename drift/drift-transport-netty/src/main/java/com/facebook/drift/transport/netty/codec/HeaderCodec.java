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
package com.facebook.drift.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class HeaderCodec
        extends ChannelDuplexHandler
{
    @Override
    public void channelRead(ChannelHandlerContext context, Object message)
    {
        if (message instanceof ByteBuf) {
            ByteBuf request = (ByteBuf) message;
            if (request.isReadable()) {
                context.fireChannelRead(HeaderTransport.decodeFrame(context.alloc(), request));
                return;
            }
        }
        context.fireChannelRead(message);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise)
    {
        if (message instanceof ThriftFrame) {
            context.write(HeaderTransport.encodeFrame(context.alloc(), (ThriftFrame) message), promise);
        }
        else {
            context.write(message, promise);
        }
    }
}
