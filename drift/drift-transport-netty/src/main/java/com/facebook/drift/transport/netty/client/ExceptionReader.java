/*
 * Copyright (C) 2017 Facebook, Inc.
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
package com.facebook.drift.transport.netty.client;

import com.facebook.drift.TApplicationException;
import com.facebook.drift.TException;
import com.facebook.drift.codec.ThriftCodec;
import com.facebook.drift.codec.ThriftCodecManager;
import com.facebook.drift.protocol.TProtocolReader;

final class ExceptionReader
{
    private static final ThriftCodec<TApplicationException> CODEC =
            new ThriftCodecManager().getCodec(TApplicationException.class);

    private ExceptionReader() {}

    public static TApplicationException readTApplicationException(TProtocolReader protocol)
            throws TException
    {
        try {
            return CODEC.read(protocol);
        }
        catch (TException e) {
            throw e;
        }
        catch (Exception e) {
            throw new TException(e);
        }
    }
}
