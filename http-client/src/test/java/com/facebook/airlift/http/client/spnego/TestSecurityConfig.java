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
package com.facebook.airlift.http.client.spnego;

import com.facebook.airlift.configuration.testing.ConfigAssertions;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Map;

public class TestSecurityConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(KerberosConfig.class)
                .setConfig(null)
                .setKeytab(null)
                .setCredentialCache(null)
                .setUseCanonicalHostname(true));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("http.authentication.krb5.config", "/etc/krb5.conf")
                .put("http.authentication.krb5.keytab", "/etc/krb5.keytab")
                .put("http.authentication.krb5.credential-cache", "/etc/krb5.ccache")
                .put("http.authentication.krb5.use-canonical-hostname", "false")
                .build();

        KerberosConfig expected = new KerberosConfig()
                .setConfig(Path.of("/etc/krb5.conf").toFile())
                .setKeytab(Path.of("/etc/krb5.keytab").toFile())
                .setCredentialCache(Path.of("/etc/krb5.ccache").toFile())
                .setUseCanonicalHostname(false);

        ConfigAssertions.assertFullMapping(properties, expected);
    }
}
