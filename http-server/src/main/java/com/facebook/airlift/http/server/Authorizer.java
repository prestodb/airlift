package com.facebook.airlift.http.server;

import java.security.Principal;
import java.util.Set;

public interface Authorizer
{
    AuthorizationResult authorize(Principal principal, Set<String> allowedRoles);
}
