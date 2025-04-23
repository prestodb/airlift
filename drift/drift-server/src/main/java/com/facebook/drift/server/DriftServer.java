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
package com.facebook.drift.server;

import com.facebook.drift.codec.ThriftCodecManager;
import com.facebook.drift.server.stats.MethodInvocationStatsFactory;
import com.facebook.drift.transport.server.ServerTransport;
import com.facebook.drift.transport.server.ServerTransportFactory;
import com.google.common.collect.ImmutableList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import java.util.Set;

import static java.util.Objects.requireNonNull;

public class DriftServer
{
    private final ServerTransport serverTransport;

    @Inject
    public DriftServer(
            ServerTransportFactory serverTransportFactory,
            ThriftCodecManager codecManager,
            MethodInvocationStatsFactory methodInvocationStatsFactory,
            Set<DriftService> services,
            Set<MethodInvocationFilter> filters)
    {
        requireNonNull(serverTransportFactory, "serverTransportFactory is null");
        requireNonNull(codecManager, "codecManager is null");
        requireNonNull(services, "services is null");

        DriftServerMethodInvoker methodInvoker = new DriftServerMethodInvoker(codecManager, services, ImmutableList.copyOf(filters), methodInvocationStatsFactory);
        serverTransport = serverTransportFactory.createServerTransport(methodInvoker);
    }

    public ServerTransport getServerTransport()
    {
        return serverTransport;
    }

    @PostConstruct
    public void start()
    {
        serverTransport.start();
    }

    @PreDestroy
    public void shutdown()
    {
        serverTransport.shutdown();
    }
}
