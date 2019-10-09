package com.facebook.airlift.http.client.spnego;

import java.net.URI;
import java.net.URISyntaxException;

final class UriUtil
{
    private UriUtil() {}

    public static URI normalizedUri(URI uri)
    {
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
