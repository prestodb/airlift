package com.facebook.airlift.dbpool;

import com.facebook.airlift.configuration.testing.ConfigAssertions;
import com.facebook.airlift.units.Duration;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestMySqlDataSourceConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(MySqlDataSourceConfig.class)
                .setDefaultFetchSize(100)
                .setMaxConnections(10)
                .setMaxConnectionWait(new Duration(500, TimeUnit.MILLISECONDS)));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("db.fetch-size", "500")
                .put("db.connections.max", "12")
                .put("db.connections.wait", "42s")
                .build();

        MySqlDataSourceConfig expected = new MySqlDataSourceConfig()
                .setDefaultFetchSize(500)
                .setMaxConnections(12)
                .setMaxConnectionWait(new Duration(42, TimeUnit.SECONDS));

        ConfigAssertions.assertFullMapping(properties, expected);
    }
}
