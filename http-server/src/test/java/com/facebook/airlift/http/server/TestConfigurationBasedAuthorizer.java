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
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestConfigurationBasedAuthorizer
{
    @Test
    public void testAuthorize()
            throws IOException
    {
        Authorizer authorizer = new ConfigurationBasedAuthorizer("src/test/resources/roles.properties");
        assertTrue(authorizer.authorize(() -> "tom", ImmutableSet.of("user"), "").isAllowed());
        assertTrue(authorizer.authorize(() -> "jerry", ImmutableSet.of("user"), "").isAllowed());
        assertFalse(authorizer.authorize(() -> "jacob", ImmutableSet.of("user"), "").isAllowed());
        assertTrue(authorizer.authorize(() -> "jacob", ImmutableSet.of("admin"), "").isAllowed());
        assertFalse(authorizer.authorize(() -> "foo.bar", ImmutableSet.of("service"), "").isAllowed());
        assertTrue(authorizer.authorize(() -> "gateway.airlift", ImmutableSet.of("service"), "").isAllowed());
        assertTrue(authorizer.authorize(() -> "logger.airlift", ImmutableSet.of("service"), "").isAllowed());
    }
}
