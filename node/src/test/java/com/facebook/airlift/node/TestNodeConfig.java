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
package com.facebook.airlift.node;

import com.facebook.airlift.configuration.testing.ConfigAssertions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import org.testng.annotations.Test;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import java.util.Map;
import java.util.UUID;

import static com.facebook.airlift.node.NodeConfig.AddressSource.HOSTNAME;
import static com.facebook.airlift.node.NodeConfig.AddressSource.IP;
import static com.facebook.airlift.testing.ValidationAssertions.assertFailsValidation;
import static com.facebook.airlift.testing.ValidationAssertions.assertValidates;
import static org.testng.Assert.assertEquals;

public class TestNodeConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(NodeConfig.class)
                .setEnvironment(null)
                .setPool("general")
                .setNodeId(null)
                .setNodeInternalAddress(null)
                .setNodeBindIp((String) null)
                .setNodeExternalAddress(null)
                .setLocation(null)
                .setBinarySpec(null)
                .setConfigSpec(null)
                .setInternalAddressSource(IP));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("node.environment", "environment")
                .put("node.pool", "pool")
                .put("node.id", "nodeId")
                .put("node.internal-address", "internal")
                .put("node.bind-ip", "10.11.12.13")
                .put("node.external-address", "external")
                .put("node.location", "location")
                .put("node.binary-spec", "binary")
                .put("node.config-spec", "config")
                .put("node.internal-address-source", "HOSTNAME")
                .build();

        NodeConfig expected = new NodeConfig()
                .setEnvironment("environment")
                .setPool("pool")
                .setNodeId("nodeId")
                .setNodeInternalAddress("internal")
                .setNodeBindIp(InetAddresses.forString("10.11.12.13"))
                .setNodeExternalAddress("external")
                .setLocation("location")
                .setBinarySpec("binary")
                .setConfigSpec("config")
                .setInternalAddressSource(HOSTNAME);

        ConfigAssertions.assertFullMapping(properties, expected);
    }

    @Test
    public void testSingleEnvironmentVariableSubstitution()
    {
        NodeConfig config = new NodeConfig()
                .setEnvironment("${ENV:TEST_ENVIRONMENT}")
                .setPool("${ENV:TEST_POOL}")
                .setNodeId("${ENV:TEST_NODE_ID}")
                .setLocation("${ENV:TEST_LOCATION}")
                .setNodeInternalAddress("${ENV:TEST_NODE_INTERNAL_ADDRESS}")
                .setNodeExternalAddress("${ENV:TEST_NODE_EXTERNAL_ADDRESS}")
                .setNodeBindIp("${ENV:TEST_NODE_BIND_IP}")
                .setBinarySpec("${ENV:TEST_BINARY_SPEC}")
                .setConfigSpec("${ENV:TEST_CONFIG_SPEC}");

        assertEquals(config.getEnvironment(), "environment");
        assertEquals(config.getPool(), "pool");
        assertEquals(config.getNodeId(), "node-id");
        assertEquals(config.getLocation(), "location");
        assertEquals(config.getNodeInternalAddress(), "node-internal-address");
        assertEquals(config.getNodeExternalAddress(), "node-external-address");
        assertEquals(config.getNodeBindIp().getHostAddress(), "10.11.12.13");
        assertEquals(config.getBinarySpec(), "binary-spec");
        assertEquals(config.getConfigSpec(), "config-spec");
    }

    @Test
    public void testMultipleEnvironmentVariableSubstitution()
    {
        NodeConfig config = new NodeConfig()
                .setEnvironment("${ENV:TEST_PREFIX}-${ENV:TEST_ENVIRONMENT}")
                .setPool("${ENV:TEST_PREFIX}-${ENV:TEST_POOL}")
                .setNodeId("${ENV:TEST_PREFIX}-${ENV:TEST_NODE_ID}")
                .setLocation("${ENV:TEST_PREFIX}-${ENV:TEST_LOCATION}")
                .setNodeInternalAddress("${ENV:TEST_PREFIX}-${ENV:TEST_NODE_INTERNAL_ADDRESS}")
                .setNodeExternalAddress("${ENV:TEST_PREFIX}-${ENV:TEST_NODE_EXTERNAL_ADDRESS}")
                .setBinarySpec("${ENV:TEST_PREFIX}-${ENV:TEST_BINARY_SPEC}")
                .setConfigSpec("${ENV:TEST_PREFIX}-${ENV:TEST_CONFIG_SPEC}");

        assertEquals(config.getEnvironment(), "test-environment");
        assertEquals(config.getPool(), "test-pool");
        assertEquals(config.getNodeId(), "test-node-id");
        assertEquals(config.getLocation(), "test-location");
        assertEquals(config.getNodeInternalAddress(), "test-node-internal-address");
        assertEquals(config.getNodeExternalAddress(), "test-node-external-address");
        assertEquals(config.getBinarySpec(), "test-binary-spec");
        assertEquals(config.getConfigSpec(), "test-config-spec");
    }

    @Test
    public void testValidations()
    {
        assertValidates(new NodeConfig()
                .setEnvironment("test")
                .setNodeId(UUID.randomUUID().toString()));

        assertFailsValidation(new NodeConfig().setNodeId("abc/123"), "nodeId", "is malformed", Pattern.class);

        assertFailsValidation(new NodeConfig(), "environment", "may not be null", NotNull.class);
        assertFailsValidation(new NodeConfig().setEnvironment("FOO"), "environment", "is malformed", Pattern.class);

        assertFailsValidation(new NodeConfig().setPool("FOO"), "pool", "is malformed", Pattern.class);
    }
}
