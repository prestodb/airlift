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

import com.facebook.airlift.configuration.Config;
import com.facebook.airlift.units.DataSize;
import com.facebook.airlift.units.Duration;
import com.facebook.airlift.units.MaxDataSize;
import com.facebook.airlift.units.MinDuration;
import com.facebook.drift.transport.netty.codec.Protocol;
import com.facebook.drift.transport.netty.codec.Transport;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.util.List;

import static com.facebook.airlift.units.DataSize.Unit.MEGABYTE;
import static com.facebook.drift.transport.netty.codec.Protocol.BINARY;
import static com.facebook.drift.transport.netty.codec.Transport.HEADER;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DriftNettyClientConfig
{
    private Transport transport = HEADER;
    private Protocol protocol = BINARY;
    private DataSize maxFrameSize = new DataSize(16, MEGABYTE);

    private Duration connectTimeout = new Duration(500, MILLISECONDS);
    private Duration requestTimeout = new Duration(10, SECONDS);

    private HostAndPort socksProxy;

    private boolean sslEnabled;
    private List<String> ciphers = ImmutableList.of();

    private File trustCertificate;
    private File key;
    private String keyPassword;
    private long sessionCacheSize = 10_000;
    private Duration sessionTimeout = new Duration(1, DAYS);

    private Boolean connectionPoolEnabled;
    private Integer connectionPoolMaxSize;
    private Integer connectionPoolMaxConnectionsPerDestination;
    private Duration connectionPoolIdleTimeout;

    private boolean tcpNoDelayEnabled;
    private boolean reuseAddressEnabled;

    @NotNull
    public Transport getTransport()
    {
        return transport;
    }

    @Config("thrift.client.transport")
    public DriftNettyClientConfig setTransport(Transport transport)
    {
        this.transport = transport;
        return this;
    }

    @NotNull
    public Protocol getProtocol()
    {
        return protocol;
    }

    @Config("thrift.client.protocol")
    public DriftNettyClientConfig setProtocol(Protocol protocol)
    {
        this.protocol = protocol;
        return this;
    }

    @NotNull
    @MinDuration("1ms")
    public Duration getConnectTimeout()
    {
        return connectTimeout;
    }

    @Config("thrift.client.connect-timeout")
    public DriftNettyClientConfig setConnectTimeout(Duration connectTimeout)
    {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @NotNull
    @MinDuration("1ms")
    public Duration getRequestTimeout()
    {
        return requestTimeout;
    }

    @Config("thrift.client.request-timeout")
    public DriftNettyClientConfig setRequestTimeout(Duration requestTimeout)
    {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public HostAndPort getSocksProxy()
    {
        return socksProxy;
    }

    @Config("thrift.client.socks-proxy")
    public DriftNettyClientConfig setSocksProxy(HostAndPort socksProxy)
    {
        this.socksProxy = socksProxy;
        return this;
    }

    @MaxDataSize("1023MB")
    public DataSize getMaxFrameSize()
    {
        return maxFrameSize;
    }

    @Config("thrift.client.max-frame-size")
    public DriftNettyClientConfig setMaxFrameSize(DataSize maxFrameSize)
    {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    public boolean isSslEnabled()
    {
        return sslEnabled;
    }

    @Config("thrift.client.ssl.enabled")
    public DriftNettyClientConfig setSslEnabled(boolean sslEnabled)
    {
        this.sslEnabled = sslEnabled;
        return this;
    }

    public File getTrustCertificate()
    {
        return trustCertificate;
    }

    @Config("thrift.client.ssl.trust-certificate")
    public DriftNettyClientConfig setTrustCertificate(File trustCertificate)
    {
        this.trustCertificate = trustCertificate;
        return this;
    }

    public File getKey()
    {
        return key;
    }

    @Config("thrift.client.ssl.key")
    public DriftNettyClientConfig setKey(File key)
    {
        this.key = key;
        return this;
    }

    public String getKeyPassword()
    {
        return keyPassword;
    }

    @Config("thrift.client.ssl.key-password")
    public DriftNettyClientConfig setKeyPassword(String keyPassword)
    {
        this.keyPassword = keyPassword;
        return this;
    }

    public long getSessionCacheSize()
    {
        return sessionCacheSize;
    }

    @Config("thrift.client.ssl.session-cache-size")
    public DriftNettyClientConfig setSessionCacheSize(long sessionCacheSize)
    {
        this.sessionCacheSize = sessionCacheSize;
        return this;
    }

    public Duration getSessionTimeout()
    {
        return sessionTimeout;
    }

    @Config("thrift.client.ssl.session-timeout")
    public DriftNettyClientConfig setSessionTimeout(Duration sessionTimeout)
    {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    public List<String> getCiphers()
    {
        return ciphers;
    }

    @Config("thrift.client.ssl.ciphers")
    public DriftNettyClientConfig setCiphers(String ciphers)
    {
        this.ciphers = Splitter
                .on(',')
                .trimResults()
                .omitEmptyStrings()
                .splitToList(requireNonNull(ciphers, "ciphers is null"));
        return this;
    }

    public Boolean getConnectionPoolEnabled()
    {
        return connectionPoolEnabled;
    }

    @Config("thrift.client.connection-pool.enabled")
    public DriftNettyClientConfig setConnectionPoolEnabled(Boolean connectionPoolEnabled)
    {
        this.connectionPoolEnabled = connectionPoolEnabled;
        return this;
    }

    @Min(1)
    public Integer getConnectionPoolMaxConnectionsPerDestination()
    {
        return connectionPoolMaxConnectionsPerDestination;
    }

    @Config("thrift.client.connection-pool.max-connections-per-destination")
    public DriftNettyClientConfig setConnectionPoolMaxConnectionsPerDestination(Integer maxConnectionsPerDestination)
    {
        this.connectionPoolMaxConnectionsPerDestination = maxConnectionsPerDestination;
        return this;
    }

    @Min(1)
    public Integer getConnectionPoolMaxSize()
    {
        return connectionPoolMaxSize;
    }

    @Config("thrift.client.connection-pool.max-size")
    public DriftNettyClientConfig setConnectionPoolMaxSize(Integer connectionPoolMaxSize)
    {
        this.connectionPoolMaxSize = connectionPoolMaxSize;
        return this;
    }

    @MinDuration("1s")
    public Duration getConnectionPoolIdleTimeout()
    {
        return connectionPoolIdleTimeout;
    }

    @Config("thrift.client.connection-pool.idle-timeout")
    public DriftNettyClientConfig setConnectionPoolIdleTimeout(Duration connectionPoolIdleTimeout)
    {
        this.connectionPoolIdleTimeout = connectionPoolIdleTimeout;
        return this;
    }

    public boolean isTcpNoDelayEnabled()
    {
        return tcpNoDelayEnabled;
    }

    @Config("thrift.client.tcp-no-delay.enabled")
    public DriftNettyClientConfig setTcpNoDelayEnabled(boolean tcpNoDelayEnabled)
    {
        this.tcpNoDelayEnabled = tcpNoDelayEnabled;
        return this;
    }

    public boolean isReuseAddressEnabled()
    {
        return reuseAddressEnabled;
    }

    @Config("thrift.client.reuse-address.enabled")
    public DriftNettyClientConfig setReuseAddressEnabled(boolean reuseAddressEnabled)
    {
        this.reuseAddressEnabled = reuseAddressEnabled;
        return this;
    }
}
