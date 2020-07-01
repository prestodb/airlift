package com.facebook.airlift.http.server;

import static java.util.Objects.requireNonNull;

public class AuthorizationResult
{
    private final boolean allowed;
    private final String reason;

    private AuthorizationResult(boolean allowed, String reason)
    {
        this.allowed = allowed;
        this.reason = requireNonNull(reason, "reason is null");
    }

    public boolean isAllowed()
    {
        return allowed;
    }

    public String getReason()
    {
        return reason;
    }

    public static AuthorizationResult success()
    {
        return new AuthorizationResult(true, "");
    }

    public static AuthorizationResult failure(String reason)
    {
        return new AuthorizationResult(false, reason);
    }
}
