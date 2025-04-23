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
package com.facebook.airlift.discovery.server;

import com.facebook.airlift.units.Duration;
import com.google.common.collect.ImmutableSet;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.facebook.airlift.discovery.server.DynamicServiceAnnouncement.toServiceWith;
import static com.facebook.airlift.discovery.server.Service.matchesPool;
import static com.facebook.airlift.discovery.server.Service.matchesType;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

@ThreadSafe
public class InMemoryDynamicStore
        implements DynamicStore
{
    private final Map<Id<Node>, Entry> descriptors = new HashMap<>();
    private final Duration maxAge;
    private final Supplier<ZonedDateTime> currentTime;

    @Inject
    public InMemoryDynamicStore(DiscoveryConfig config, Supplier<ZonedDateTime> timeSource)
    {
        this.currentTime = timeSource;
        this.maxAge = config.getMaxAge();
    }

    @Override
    public synchronized void put(Id<Node> nodeId, DynamicAnnouncement announcement)
    {
        requireNonNull(nodeId, "nodeId is null");
        requireNonNull(announcement, "announcement is null");

        Set<Service> services = ImmutableSet.copyOf(announcement.getServiceAnnouncements().stream().map(toServiceWith(nodeId, announcement.getLocation(), announcement.getPool())).collect(toImmutableList()));

        Instant expiration = currentTime.get().toInstant().plusMillis((int) maxAge.toMillis());
        descriptors.put(nodeId, new Entry(expiration, services));
    }

    @Override
    public synchronized void delete(Id<Node> nodeId)
    {
        requireNonNull(nodeId, "nodeId is null");

        descriptors.remove(nodeId);
    }

    @Override
    public synchronized Set<Service> getAll()
    {
        removeExpired();

        ImmutableSet.Builder<Service> builder = ImmutableSet.builder();
        for (Entry entry : descriptors.values()) {
            builder.addAll(entry.getServices());
        }
        return builder.build();
    }

    @Override
    public synchronized Set<Service> get(String type)
    {
        requireNonNull(type, "type is null");

        return ImmutableSet.copyOf(getAll().stream().filter(matchesType(type)).iterator());
    }

    @Override
    public synchronized Set<Service> get(String type, String pool)
    {
        requireNonNull(type, "type is null");
        requireNonNull(pool, "pool is null");

        return ImmutableSet.copyOf(getAll().stream()
                .filter(service -> matchesType(type).test(service) && matchesPool(pool).test(service))
                .iterator());
    }

    private synchronized void removeExpired()
    {
        Iterator<Entry> iterator = descriptors.values().iterator();

        ZonedDateTime now = currentTime.get();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();

            if (now.toInstant().isAfter(entry.getExpiration())) {
                iterator.remove();
            }
        }
    }

    private static class Entry
    {
        private final Set<Service> services;
        private final Instant expiration;

        public Entry(Instant expiration, Set<Service> services)
        {
            this.expiration = expiration;
            this.services = ImmutableSet.copyOf(services);
        }

        public Instant getExpiration()
        {
            return expiration;
        }

        public Set<Service> getServices()
        {
            return services;
        }
    }
}
