/*
 * Copyright 2010 Proofpoint, Inc.
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
package com.facebook.airlift.discovery.client;

import com.facebook.airlift.http.client.CacheControl;
import com.facebook.airlift.http.client.HttpClient;
import com.facebook.airlift.http.client.Request;
import com.facebook.airlift.http.client.Request.Builder;
import com.facebook.airlift.http.client.RequestStats;
import com.facebook.airlift.http.client.Response;
import com.facebook.airlift.http.client.ResponseHandler;
import com.facebook.airlift.json.JsonCodec;
import com.facebook.airlift.node.NodeInfo;
import com.facebook.airlift.units.Duration;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.inject.Inject;
import org.weakref.jmx.Flatten;
import org.weakref.jmx.Managed;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.facebook.airlift.discovery.client.DiscoveryAnnouncementClient.DEFAULT_DELAY;
import static com.facebook.airlift.http.client.HttpStatus.NOT_MODIFIED;
import static com.facebook.airlift.http.client.HttpStatus.OK;
import static com.facebook.airlift.http.client.Request.Builder.prepareGet;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class HttpDiscoveryLookupClient
        implements DiscoveryLookupClient
{
    private final String environment;
    private final Supplier<URI> discoveryServiceURI;
    private final NodeInfo nodeInfo;
    private final JsonCodec<ServiceDescriptorsRepresentation> serviceDescriptorsCodec;
    private final HttpClient httpClient;

    @Inject
    public HttpDiscoveryLookupClient(
            @ForDiscoveryClient Supplier<URI> discoveryServiceURI,
            NodeInfo nodeInfo,
            JsonCodec<ServiceDescriptorsRepresentation> serviceDescriptorsCodec,
            @ForDiscoveryClient HttpClient httpClient)
    {
        requireNonNull(discoveryServiceURI, "discoveryServiceURI is null");
        requireNonNull(nodeInfo, "nodeInfo is null");
        requireNonNull(serviceDescriptorsCodec, "serviceDescriptorsCodec is null");
        requireNonNull(httpClient, "httpClient is null");

        this.nodeInfo = nodeInfo;
        this.environment = nodeInfo.getEnvironment();
        this.discoveryServiceURI = discoveryServiceURI;
        this.serviceDescriptorsCodec = serviceDescriptorsCodec;
        this.httpClient = httpClient;
    }

    @Flatten
    @Managed
    public RequestStats getStats()
    {
        return httpClient.getStats();
    }

    @Override
    public ListenableFuture<ServiceDescriptors> getServices(String type)
    {
        requireNonNull(type, "type is null");
        return lookup(type, null, null);
    }

    @Override
    public ListenableFuture<ServiceDescriptors> getServices(String type, String pool)
    {
        requireNonNull(type, "type is null");
        requireNonNull(pool, "pool is null");
        return lookup(type, pool, null);
    }

    @Override
    public ListenableFuture<ServiceDescriptors> refreshServices(ServiceDescriptors serviceDescriptors)
    {
        requireNonNull(serviceDescriptors, "serviceDescriptors is null");
        return lookup(serviceDescriptors.getType(), serviceDescriptors.getPool(), serviceDescriptors);
    }

    private ListenableFuture<ServiceDescriptors> lookup(final String type, final String pool, final ServiceDescriptors serviceDescriptors)
    {
        requireNonNull(type, "type is null");

        URI uri = discoveryServiceURI.get();
        if (uri == null) {
            return immediateFailedFuture(new DiscoveryException("No discovery servers are available"));
        }

        uri = URI.create(uri + "/v1/service/" + type + "/");
        if (pool != null) {
            uri = uri.resolve(pool);
        }

        Builder requestBuilder = prepareGet()
                .setUri(uri)
                .setHeader("User-Agent", nodeInfo.getNodeId());
        if (serviceDescriptors != null && serviceDescriptors.getETag() != null) {
            requestBuilder.setHeader(HttpHeaders.ETAG, serviceDescriptors.getETag());
        }
        return httpClient.executeAsync(requestBuilder.build(), new DiscoveryResponseHandler<ServiceDescriptors>(format("Lookup of %s", type), uri)
        {
            @Override
            public ServiceDescriptors handle(Request request, Response response)
            {
                Duration maxAge = extractMaxAge(response);
                String eTag = response.getHeader(HttpHeaders.ETAG);

                if (NOT_MODIFIED.code() == response.getStatusCode() && serviceDescriptors != null) {
                    return new ServiceDescriptors(serviceDescriptors, maxAge, eTag);
                }

                if (OK.code() != response.getStatusCode()) {
                    throw new DiscoveryException(format("Lookup of %s failed with status code %s", type, response.getStatusCode()));
                }

                byte[] json;
                try {
                    json = response.getInputStream().readAllBytes();
                }
                catch (IOException e) {
                    throw new DiscoveryException(format("Lookup of %s failed", type), e);
                }

                ServiceDescriptorsRepresentation serviceDescriptorsRepresentation = serviceDescriptorsCodec.fromJson(json);
                if (!environment.equals(serviceDescriptorsRepresentation.getEnvironment())) {
                    throw new DiscoveryException(format("Expected environment to be %s, but was %s", environment, serviceDescriptorsRepresentation.getEnvironment()));
                }

                return new ServiceDescriptors(
                        type,
                        pool,
                        serviceDescriptorsRepresentation.getServiceDescriptors(),
                        maxAge,
                        eTag);
            }
        });
    }

    private Duration extractMaxAge(Response response)
    {
        String header = response.getHeader(HttpHeaders.CACHE_CONTROL);
        if (header != null) {
            CacheControl cacheControl = CacheControl.valueOf(header);
            if (cacheControl.getMaxAge() > 0) {
                return new Duration(cacheControl.getMaxAge(), TimeUnit.SECONDS);
            }
        }
        return DEFAULT_DELAY;
    }

    private class DiscoveryResponseHandler<T>
            implements ResponseHandler<T, DiscoveryException>
    {
        private final String name;
        private final URI uri;

        protected DiscoveryResponseHandler(String name, URI uri)
        {
            this.name = name;
            this.uri = uri;
        }

        @Override
        public T handle(Request request, Response response)
        {
            return null;
        }

        @Override
        public final T handleException(Request request, Exception exception)
        {
            if (exception instanceof InterruptedException) {
                throw new DiscoveryException(name + " was interrupted for " + uri);
            }
            if (exception instanceof CancellationException) {
                throw new DiscoveryException(name + " was canceled for " + uri);
            }
            if (exception instanceof DiscoveryException) {
                throw (DiscoveryException) exception;
            }

            throw new DiscoveryException(name + " failed for " + uri, exception);
        }
    }
}
