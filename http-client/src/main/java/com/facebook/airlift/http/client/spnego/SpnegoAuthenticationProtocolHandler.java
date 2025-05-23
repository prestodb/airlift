package com.facebook.airlift.http.client.spnego;

import org.eclipse.jetty.client.AuthenticationProtocolHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.client.Result;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;

import java.util.concurrent.CompletableFuture;

/**
 * Jetty {@link AuthenticationProtocolHandler} requires that the
 * {@code "WWW-Authenticate"} header contains a realm, but the
 * {@code "Negotiate"} scheme will not have one. This hacks around that by
 * adding a new header value for that scheme which contains a dummy realm,
 * allowing the rest of the authentication handling to work as normal.
 * Unfortunately, there is no easier way as all of the related code in
 * that class is private.
 */
public class SpnegoAuthenticationProtocolHandler
        extends WWWAuthenticationProtocolHandler
{
    private static final String NEGOTIATE = HttpHeader.NEGOTIATE.asString();

    public SpnegoAuthenticationProtocolHandler(HttpClient client)
    {
        super(client);
    }

    @Override
    public String getName()
    {
        return "spnego";
    }

    @Override
    public Response.Listener getResponseListener()
    {
        return new ForwardingResponseListener(super.getResponseListener())
        {
            @Override
            public void onComplete(Result result)
            {
                HttpHeader header = getAuthenticateHeader();
                Response response = result.getResponse();

                super.onComplete(new Result(result.getRequest(), result.getRequestFailure(), new Response()
                {
                    @Override
                    public Request getRequest()
                    {
                        return response.getRequest();
                    }

                    @Override
                    public HttpVersion getVersion()
                    {
                        return response.getVersion();
                    }

                    @Override
                    public int getStatus()
                    {
                        return response.getStatus();
                    }

                    @Override
                    public String getReason()
                    {
                        return response.getReason();
                    }

                    @Override
                    public HttpFields getHeaders()
                    {
                        // There is no need to check for a SPNEGO token because it can't exist here:
                        // * SPNEGO token only exist if the client successfully authenticated
                        // * This authenticate handler is not called when already authenticated
                        return HttpFields.build(response.getHeaders(), new HttpField(header, NEGOTIATE + " realm=\"dummy\""));
                    }

                    @Override
                    public HttpFields getTrailers()
                    {
                        return response.getTrailers();
                    }

                    @Override
                    public CompletableFuture<Boolean> abort(Throwable cause)
                    {
                        return response.abort(cause);
                    }
                }, result.getResponseFailure()));
            }
        };
    }
}
