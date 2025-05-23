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
package com.facebook.airlift.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import static com.facebook.airlift.sample.PersonRepresentation.from;
import static java.util.Objects.requireNonNull;

@Path("/v1/person/{id: \\w+}")
public class PersonResource
{
    private final PersonStore store;

    @Inject
    public PersonResource(PersonStore store)
    {
        requireNonNull(store, "store must not be null");

        this.store = store;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") String id, @Context UriInfo uriInfo)
    {
        requireNonNull(id, "id must not be null");

        Person person = store.get(id);

        if (person == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("[" + id + "]").build();
        }

        return Response.ok(from(person, uriInfo.getRequestUri())).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("id") String id, PersonRepresentation person)
    {
        requireNonNull(id, "id must not be null");
        requireNonNull(person, "person must not be null");

        boolean added = store.put(id, person.toPerson());
        if (added) {
            UriBuilder uri = UriBuilder.fromResource(PersonResource.class);
            return Response.created(uri.build(id)).build();
        }

        return Response.noContent().build();
    }

    @DELETE
    public Response delete(@PathParam("id") String id)
    {
        requireNonNull(id, "id must not be null");

        if (!store.delete(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.noContent().build();
    }
}
