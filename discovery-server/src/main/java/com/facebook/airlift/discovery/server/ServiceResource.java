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

import com.facebook.airlift.node.NodeInfo;
import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static com.google.common.collect.Sets.union;

@Path("/v1/service")
public class ServiceResource
{
    private final DynamicStore dynamicStore;
    private final StaticStore staticStore;
    private final NodeInfo node;

    @Inject
    public ServiceResource(DynamicStore dynamicStore, StaticStore staticStore, NodeInfo node)
    {
        this.dynamicStore = dynamicStore;
        this.staticStore = staticStore;
        this.node = node;
    }

    @GET
    @Path("{type}/{pool}")
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices(@PathParam("type") String type, @PathParam("pool") String pool)
    {
        return new Services(node.getEnvironment(), union(dynamicStore.get(type, pool), staticStore.get(type, pool)));
    }

    @GET
    @Path("{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices(@PathParam("type") String type)
    {
        return new Services(node.getEnvironment(), union(dynamicStore.get(type), staticStore.get(type)));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices()
    {
        return new Services(node.getEnvironment(), union(dynamicStore.getAll(), staticStore.getAll()));
    }
}
