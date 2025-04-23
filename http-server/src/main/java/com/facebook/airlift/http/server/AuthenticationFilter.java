/*
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static java.io.OutputStream.nullOutputStream;
import static java.util.Objects.requireNonNull;

public class AuthenticationFilter
        implements Filter
{
    private final List<Authenticator> authenticators;

    @Inject
    public AuthenticationFilter(List<Authenticator> authenticators)
    {
        this.authenticators = ImmutableList.copyOf(requireNonNull(authenticators, "authenticators is null"));
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain nextFilter)
            throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // skip authentication if non-secure or not configured
        if (!request.isSecure() || authenticators.isEmpty()) {
            nextFilter.doFilter(request, response);
            return;
        }

        // try to authenticate, collecting errors and authentication headers
        Set<String> messages = new LinkedHashSet<>();
        Set<String> authenticateHeaders = new LinkedHashSet<>();

        for (Authenticator authenticator : authenticators) {
            Principal principal;
            try {
                principal = authenticator.authenticate(request);
            }
            catch (AuthenticationException e) {
                if (e.getMessage() != null) {
                    messages.add(e.getMessage());
                }
                e.getAuthenticateHeader().ifPresent(authenticateHeaders::add);
                continue;
            }

            // authentication succeeded
            nextFilter.doFilter(withPrincipal(request, principal), response);
            return;
        }

        // authentication failed
        skipRequestBody(request);

        for (String value : authenticateHeaders) {
            response.addHeader(WWW_AUTHENTICATE, value);
        }

        if (messages.isEmpty()) {
            messages.add("Unauthorized");
        }
        // The error string is used by clients for exception messages and
        // is presented to the end user, thus it should be a single line.
        String error = Joiner.on(" | ").join(messages);

        // Clients should use the response body rather than the HTTP status
        // message (which does not exist with HTTP/2), but the status message
        // still needs to be sent for compatibility with existing clients.
        response.setStatus(SC_UNAUTHORIZED);
        response.setContentType(PLAIN_TEXT_UTF_8.toString());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(error);
        }
    }

    private static ServletRequest withPrincipal(HttpServletRequest request, Principal principal)
    {
        requireNonNull(principal, "principal is null");
        return new HttpServletRequestWrapper(request)
        {
            @Override
            public Principal getUserPrincipal()
            {
                return principal;
            }
        };
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
            inputStream.transferTo(nullOutputStream());
        }
    }
}
