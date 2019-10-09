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

import org.testng.annotations.Test;

import static com.facebook.airlift.http.server.Inet4Networks.isPrivateNetworkAddress;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestInet4Networks
{
    @Test
    public void test()
    {
        assertTrue(isPrivateNetworkAddress("127.0.0.1"));
        assertTrue(isPrivateNetworkAddress("127.1.2.3"));
        assertTrue(isPrivateNetworkAddress("169.254.0.1"));
        assertTrue(isPrivateNetworkAddress("169.254.1.2"));
        assertTrue(isPrivateNetworkAddress("192.168.0.1"));
        assertTrue(isPrivateNetworkAddress("192.168.1.2"));
        assertTrue(isPrivateNetworkAddress("172.16.0.1"));
        assertTrue(isPrivateNetworkAddress("172.16.1.2"));
        assertTrue(isPrivateNetworkAddress("172.16.1.2"));
        assertTrue(isPrivateNetworkAddress("10.0.0.1"));
        assertTrue(isPrivateNetworkAddress("10.1.2.3"));

        assertFalse(isPrivateNetworkAddress("1.2.3.4"));
        assertFalse(isPrivateNetworkAddress("172.33.0.0"));
    }
}
