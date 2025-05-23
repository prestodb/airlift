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
package com.facebook.airlift.dbpool;

import com.facebook.airlift.units.Duration;

import javax.sql.PooledConnection;

import java.sql.SQLException;

public class MockManagedDataSource
        extends ManagedDataSource
{
    private final MockConnectionPoolDataSource poolDataSource;

    public MockManagedDataSource(int maxConnections, Duration maxConnectionWait)
    {
        this(new MockConnectionPoolDataSource(), maxConnections, maxConnectionWait);
    }

    public MockManagedDataSource(MockConnectionPoolDataSource poolDataSource, int maxConnections, Duration maxConnectionWait)
    {
        super(maxConnections, maxConnectionWait);
        this.poolDataSource = poolDataSource;
    }

    @Override
    protected PooledConnection createConnectionInternal()
            throws SQLException
    {
        return poolDataSource.getPooledConnection();
    }
}
