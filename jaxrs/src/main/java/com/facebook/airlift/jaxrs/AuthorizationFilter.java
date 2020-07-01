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
package com.facebook.airlift.jaxrs;

import com.facebook.airlift.http.server.AuthorizationResult;
import com.facebook.airlift.http.server.Authorizer;
import com.facebook.airlift.http.server.HttpServerConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import static com.facebook.airlift.http.server.HttpServerConfig.AuthorizationPolicy;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@Provider
public class AuthorizationFilter
        implements ContainerRequestFilter
{
    private final Authorizer authorizer;
    private final AuthorizationPolicy authorizationPolicy;
    private final Set<String> defaultAllowedRoles;

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    public AuthorizationFilter(Authorizer authorizer, HttpServerConfig httpServerConfig)
    {
        this(
                authorizer,
                httpServerConfig.getDefaultAuthorizationPolicy(),
                httpServerConfig.getDefaultAllowedRoles());
    }

    @VisibleForTesting
    public AuthorizationFilter(
            Authorizer authorizer,
            AuthorizationPolicy authorizationPolicy,
            Set<String> defaultAllowedRoles)
    {
        this.authorizer = requireNonNull(authorizer, "authorizer is null");
        this.authorizationPolicy = requireNonNull(authorizationPolicy, "authorizationPolicy is null");
        this.defaultAllowedRoles = requireNonNull(defaultAllowedRoles, "defaultAllowedRoles is null");
    }

    @Override
    public void filter(ContainerRequestContext request)
    {
        Principal principal = request.getSecurityContext().getUserPrincipal();
        if (principal == null) {
            request.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Request principal is missing.")
                    .build());
            return;
        }

        Optional<Set<String>> allowedRoles = getAllowedRoles();
        if (!allowedRoles.isPresent()) {
            switch (authorizationPolicy) {
                case ALLOW:
                    return;
                case DENY:
                    request.abortWith(Response.status(Response.Status.FORBIDDEN)
                            .entity(format("Principal %s is not allowed to access the resource. Reason: denied by default policy",
                                    principal.getName()))
                            .build());
                    return;
                case DEFAULT_ROLES:
                    allowedRoles = Optional.of(defaultAllowedRoles);
                    break;
                default:
            }
        }

        AuthorizationResult result = authorizer.authorize(principal, allowedRoles.get());
        if (!result.isAllowed()) {
            request.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity(format("Principal %s is not allowed to access the resource. Reason: %s",
                            principal.getName(),
                            result.getReason()))
                    .build());
        }
    }

    private Optional<Set<String>> getAllowedRoles()
    {
        // Checking method level annotation for roles (overrides the class level annotation if exists)
        if (resourceInfo.getResourceMethod().isAnnotationPresent(RolesAllowed.class)) {
            return Optional.of(ImmutableSet.copyOf(resourceInfo.getResourceMethod().getAnnotation(RolesAllowed.class).value()));
        }
        // Checking class level annotation for roles
        else if (resourceInfo.getResourceClass().isAnnotationPresent(RolesAllowed.class)) {
            return Optional.of(ImmutableSet.copyOf(resourceInfo.getResourceClass().getAnnotation(RolesAllowed.class).value()));
        }
        else {
            return Optional.empty();
        }
    }
}
