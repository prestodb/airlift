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
package com.facebook.airlift.http.server;

import com.google.common.collect.ImmutableSet;

import javax.annotation.security.RolesAllowed;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import static com.facebook.airlift.http.server.HttpServerConfig.AuthorizationPolicy;
import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.nullOutputStream;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class AuthorizationEnabledServlet
        extends HttpServlet
{
    private final Servlet delegate;
    private final Authorizer authorizer;
    private final AuthorizationPolicy authorizationPolicy;
    private final Set<String> defaultAllowedRoles;
    private final Optional<Set<String>> allowedRoles;

    public AuthorizationEnabledServlet(
            Servlet delegate,
            Authorizer authorizer,
            AuthorizationPolicy authorizationPolicy,
            Set<String> defaultAllowedRoles)
    {
        this.delegate = requireNonNull(delegate, "delegate is null");
        this.authorizer = requireNonNull(authorizer, "authorizer is null");
        this.authorizationPolicy = requireNonNull(authorizationPolicy, "authorizationPolicy is null");
        this.defaultAllowedRoles = requireNonNull(defaultAllowedRoles, "defaultAllowedRoles is null");
        this.allowedRoles = getRolesFromClassMetadata(delegate);
    }

    @Override
    public void init()
            throws ServletException
    {
        super.init();
        delegate.init(this.getServletConfig());
    }

    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            abortWithMessage(request, response, "Request principal is missing.");
            return;
        }

        Optional<Set<String>> allowedRoles = this.allowedRoles;
        if (!allowedRoles.isPresent()) {
            switch (authorizationPolicy) {
                case ALLOW:
                    delegate.service(req, res);
                    return;
                case DENY:
                    abortWithMessage(request, response, format("Principal %s is not allowed to access the resource. Reason: denied by default policy",
                            principal.getName()));
                    return;
                case DEFAULT_ROLES:
                    allowedRoles = Optional.of(defaultAllowedRoles);
                    break;
                default:
            }
        }

        AuthorizationResult result = authorizer.authorize(
                principal,
                allowedRoles.get(),
                request.getRequestURL().toString());
        if (!result.isAllowed()) {
            abortWithMessage(request, response, format("Principal %s is not allowed to access the resource. Reason: %s",
                    principal.getName(),
                    result.getReason()));
            return;
        }
        delegate.service(req, res);
    }

    private static void abortWithMessage(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException
    {
        skipRequestBody(request);
        response.sendError(HttpServletResponse.SC_FORBIDDEN, format(message));
    }

    private static void skipRequestBody(HttpServletRequest request)
            throws IOException
    {
        // If we send the challenge without consuming the body of the request,
        // the server will close the connection after sending the response.
        // The client may interpret this as a failed request and not resend the
        // request with the authentication header. We can avoid this behavior
        // in the client by reading and discarding the entire body of the
        // unauthenticated request before sending the response.
        try (InputStream inputStream = request.getInputStream()) {
            copy(inputStream, nullOutputStream());
        }
    }

    private static Optional<Set<String>> getRolesFromClassMetadata(Servlet servlet)
    {
        if (servlet.getClass().isAnnotationPresent(RolesAllowed.class)) {
            return Optional.of(ImmutableSet.copyOf(servlet.getClass().getAnnotation(RolesAllowed.class).value()));
        }
        else {
            return Optional.empty();
        }
    }
}
