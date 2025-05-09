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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;

import java.util.Set;

import static java.util.Objects.requireNonNull;

@Immutable
public class Services
{
    private final String environment;
    private final Set<Service> services;

    public Services(String environment, Set<Service> services)
    {
        this.environment = requireNonNull(environment, "environment is null");
        this.services = ImmutableSet.copyOf(requireNonNull(services, "services is null"));
    }

    @JsonProperty
    public String getEnvironment()
    {
        return environment;
    }

    @JsonProperty
    public Set<Service> getServices()
    {
        return services;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Services services1 = (Services) o;

        if (!environment.equals(services1.environment)) {
            return false;
        }
        if (!services.equals(services1.services)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = environment.hashCode();
        result = 31 * result + services.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "Services{" +
                "environment='" + environment + '\'' +
                ", services=" + services +
                '}';
    }
}
