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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.function.Function;

public class DynamicServiceAnnouncement
{
    private final Id<Service> id;
    private final String type;
    private final Map<String, String> properties;

    @JsonCreator
    public DynamicServiceAnnouncement(
            @JsonProperty("id") Id<Service> id,
            @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, String> properties)
    {
        this.id = id;
        this.type = type;

        if (properties != null) {
            this.properties = ImmutableMap.copyOf(properties);
        }
        else {
            this.properties = null;
        }
    }

    @NotNull
    public Id<Service> getId()
    {
        return id;
    }

    @NotNull
    public String getType()
    {
        return type;
    }

    @NotNull
    public Map<String, String> getProperties()
    {
        return properties;
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

        DynamicServiceAnnouncement that = (DynamicServiceAnnouncement) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "ServiceAnnouncement{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                '}';
    }

    public static Function<DynamicServiceAnnouncement, Service> toServiceWith(final Id<Node> nodeId, final String location, final String pool)
    {
        return announcement -> new Service(announcement.getId(), nodeId, announcement.getType(), pool, location, announcement.getProperties());
    }
}
