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
package com.facebook.drift.transport.netty.client;

import com.facebook.airlift.units.DataSize;
import com.facebook.airlift.units.Duration;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Map;

import static com.facebook.airlift.configuration.testing.ConfigAssertions.assertFullMapping;
import static com.facebook.airlift.configuration.testing.ConfigAssertions.assertRecordedDefaults;
import static com.facebook.airlift.configuration.testing.ConfigAssertions.recordDefaults;
import static com.facebook.airlift.units.DataSize.Unit.MEGABYTE;
import static com.facebook.drift.transport.netty.codec.Protocol.BINARY;
import static com.facebook.drift.transport.netty.codec.Protocol.COMPACT;
import static com.facebook.drift.transport.netty.codec.Transport.FRAMED;
import static com.facebook.drift.transport.netty.codec.Transport.HEADER;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class TestDriftNettyClientConfig
{
    @Test
    public void testDefaults()
    {
        assertRecordedDefaults(recordDefaults(DriftNettyClientConfig.class)
                .setTransport(HEADER)
                .setProtocol(BINARY)
                .setConnectTimeout(new Duration(500, MILLISECONDS))
                .setRequestTimeout(new Duration(10, SECONDS))
                .setSocksProxy(null)
                .setMaxFrameSize(new DataSize(16, MEGABYTE))
                .setSslEnabled(false)
                .setTrustCertificate(null)
                .setKey(null)
                .setKeyPassword(null)
                .setSessionCacheSize(10_000)
                .setSessionTimeout(new Duration(1, DAYS))
                .setCiphers("")
                .setConnectionPoolEnabled(null)
                .setConnectionPoolMaxConnectionsPerDestination(null)
                .setConnectionPoolMaxSize(null)
                .setConnectionPoolIdleTimeout(null)
                .setTcpNoDelayEnabled(false)
                .setReuseAddressEnabled(false));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("thrift.client.transport", "FRAMED")
                .put("thrift.client.protocol", "COMPACT")
                .put("thrift.client.connect-timeout", "99ms")
                .put("thrift.client.request-timeout", "33m")
                .put("thrift.client.socks-proxy", "localhost:11")
                .put("thrift.client.max-frame-size", "55MB")
                .put("thrift.client.ssl.enabled", "true")
                .put("thrift.client.ssl.trust-certificate", "trust")
                .put("thrift.client.ssl.key", "key")
                .put("thrift.client.ssl.key-password", "key_password")
                .put("thrift.client.ssl.session-cache-size", "678")
                .put("thrift.client.ssl.session-timeout", "78h")
                .put("thrift.client.ssl.ciphers", "some_cipher")
                .put("thrift.client.connection-pool.enabled", "true")
                .put("thrift.client.connection-pool.max-connections-per-destination", "123")
                .put("thrift.client.connection-pool.max-size", "321")
                .put("thrift.client.connection-pool.idle-timeout", "12m")
                .put("thrift.client.tcp-no-delay.enabled", "true")
                .put("thrift.client.reuse-address.enabled", "true")
                .build();

        DriftNettyClientConfig expected = new DriftNettyClientConfig()
                // testing a Transport that is not the default (HEADER)
                .setTransport(FRAMED)
                .setProtocol(COMPACT)
                .setConnectTimeout(new Duration(99, MILLISECONDS))
                .setRequestTimeout(new Duration(33, MINUTES))
                .setSocksProxy(HostAndPort.fromParts("localhost", 11))
                .setMaxFrameSize(new DataSize(55, MEGABYTE))
                .setSslEnabled(true)
                .setTrustCertificate(Paths.get("trust").toFile())
                .setKey(Paths.get("key").toFile())
                .setKeyPassword("key_password")
                .setSessionCacheSize(678)
                .setSessionTimeout(new Duration(78, HOURS))
                .setCiphers("some_cipher")
                .setConnectionPoolEnabled(true)
                .setConnectionPoolMaxConnectionsPerDestination(123)
                .setConnectionPoolMaxSize(321)
                .setConnectionPoolIdleTimeout(new Duration(12, MINUTES))
                .setTcpNoDelayEnabled(true)
                .setReuseAddressEnabled(true);

        assertFullMapping(properties, expected);
    }
}
