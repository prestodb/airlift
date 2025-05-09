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

import com.facebook.airlift.configuration.testing.ConfigAssertions;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Map;

public class TestKerberosConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(KerberosConfig.class)
                .setKerberosConfig(null)
                .setServiceName(null)
                .setKeytab(null)
                .setPrincipalHostname(null));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("http.authentication.krb5.config", "/etc/krb5.conf")
                .put("http.server.authentication.krb5.service-name", "airlift")
                .put("http.server.authentication.krb5.keytab", "/tmp/presto.keytab")
                .put("http.authentication.krb5.principal-hostname", "presto.example.com")
                .build();

        KerberosConfig expected = new KerberosConfig()
                .setKerberosConfig(Path.of("/etc/krb5.conf").toFile())
                .setServiceName("airlift")
                .setKeytab(Path.of("/tmp/presto.keytab").toFile())
                .setPrincipalHostname("presto.example.com");

        ConfigAssertions.assertFullMapping(properties, expected);
    }
}
