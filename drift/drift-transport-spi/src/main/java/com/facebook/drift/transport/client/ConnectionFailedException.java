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
package com.facebook.drift.transport.client;

import com.facebook.drift.protocol.TTransportException;

import static java.util.Objects.requireNonNull;

public class ConnectionFailedException
        extends TTransportException
{
    private final Address address;

    public ConnectionFailedException(Address address, Throwable cause)
    {
        super("Failed to connect to " + address, requireNonNull(cause, "cause is null"));
        this.address = requireNonNull(address, "address is null");
    }

    public Address getAddress()
    {
        return address;
    }
}
