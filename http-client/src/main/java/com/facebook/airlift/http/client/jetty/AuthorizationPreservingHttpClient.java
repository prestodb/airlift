package com.facebook.airlift.http.client.jetty;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.URI;

import static org.eclipse.jetty.http.HttpHeader.AUTHORIZATION;

class AuthorizationPreservingHttpClient
        extends HttpClient
{
    private static final String PRESERVE_AUTHORIZATION_KEY = "airlift_preserve_authorization";

    public AuthorizationPreservingHttpClient(HttpClientTransport transport, SslContextFactory.Client sslContextFactory)
    {
        super(transport);
        this.setSslContextFactory(sslContextFactory);
    }

    @Override
    protected Request copyRequest(Request oldRequest, URI newURI)
    {
        Request newRequest = super.copyRequest(oldRequest, newURI);

        if (isPreserveAuthorization(oldRequest)) {
            setPreserveAuthorization(newRequest, true);
            for (HttpField field : oldRequest.getHeaders()) {
                if (field.getHeader() == AUTHORIZATION) {
                    newRequest.headers(headers -> headers.add(field.getName(), field.getValue()));
                }
            }
        }

        return newRequest;
    }

    public static void setPreserveAuthorization(Request request, boolean preserveAuthorization)
    {
        request.attribute(PRESERVE_AUTHORIZATION_KEY, preserveAuthorization);
    }

    private static boolean isPreserveAuthorization(Request request)
    {
        return (boolean) request.getAttributes().get(PRESERVE_AUTHORIZATION_KEY);
    }
}
