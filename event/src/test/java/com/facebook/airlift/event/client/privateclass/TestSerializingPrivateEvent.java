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
package com.facebook.airlift.event.client.privateclass;

import com.facebook.airlift.event.client.EventField;
import com.facebook.airlift.event.client.EventType;
import com.facebook.airlift.event.client.JsonEventSerializer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.google.common.io.ByteStreams.nullOutputStream;

public class TestSerializingPrivateEvent
{
    @Test
    public void testSerialize()
            throws IOException
    {
        JsonEventSerializer serializer = new JsonEventSerializer(PrivateEvent.class);
        JsonGenerator generator = new JsonFactory().createGenerator(nullOutputStream());
        serializer.serialize(new PrivateEvent(), generator);
    }

    @EventType("Private")
    private static class PrivateEvent
    {
        @EventField
        public String getName()
        {
            return "private";
        }
    }
}
