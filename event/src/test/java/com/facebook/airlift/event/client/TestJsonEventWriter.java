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
package com.facebook.airlift.event.client;

import com.facebook.airlift.event.client.NestedDummyEventClass.NestedPart;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static com.facebook.airlift.event.client.ChainedCircularEventClass.ChainedPart;
import static com.facebook.airlift.event.client.EventTypeMetadata.getValidEventTypeMetaDataSet;
import static java.io.OutputStream.nullOutputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class TestJsonEventWriter
{
    private JsonEventWriter eventWriter;

    @BeforeClass
    public void setup()
            throws Exception
    {
        Set<EventTypeMetadata<?>> eventTypes = getValidEventTypeMetaDataSet(
                FixedDummyEventClass.class,
                FixedDummyEventWithZonedDateTimeClass.class,
                NestedDummyEventClass.class,
                NestedDummyEventWithZonedDateTimeClass.class,
                CircularEventClass.class, ChainedCircularEventClass.class);
        eventWriter = new JsonEventWriter(eventTypes);
    }

    @Test
    public void testEventWriter()
            throws Exception
    {
        assertEventJson(createEventGenerator(TestingUtils.getEvents()), "events.json");
    }

    @Test
    public void testNullValue()
            throws Exception
    {
        FixedDummyEventClass event = new FixedDummyEventClass(
                "localhost", new DateTime("2011-09-09T01:59:59.999Z"), UUID.fromString("1ea8ca34-db36-11e0-b76f-8b7d505ab1ad"), 123, null);

        assertEventJson(createEventGenerator(ImmutableList.of(event)), "nullValue.json");
    }

    @Test
    public void testNullValueWithZonedDateTime()
            throws Exception
    {
        FixedDummyEventWithZonedDateTimeClass event = new FixedDummyEventWithZonedDateTimeClass(
                "localhost", ZonedDateTime.parse("2011-09-09T01:59:59.999Z"), UUID.fromString("1ea8ca34-db36-11e0-b76f-8b7d505ab1ad"), 123, null);

        assertEventJson(createEventGenerator(ImmutableList.of(event)), "nullValue.json");
    }

    @Test
    public void testNestedEvent()
            throws Exception
    {
        NestedDummyEventClass nestedEvent = new NestedDummyEventClass(
                "localhost", new DateTime("2011-09-09T01:48:08.888Z"), UUID.fromString("6b598c2a-0a95-4f3f-9298-5a4d70ca13fc"), 9999, "nested",
                ImmutableList.of("abc", "xyz"),
                new NestedPart("first", new NestedPart("second", new NestedPart("third", null))),
                ImmutableList.of(new NestedPart("listFirst", new NestedPart("listSecond", null)), new NestedPart("listThird", null)));

        assertEventJson(createEventGenerator(ImmutableList.of(nestedEvent)), "nested.json");
    }

    @Test
    public void testNestedEventWithZonedDateTime()
            throws Exception
    {
        NestedDummyEventWithZonedDateTimeClass nestedEvent = new NestedDummyEventWithZonedDateTimeClass(
                "localhost", ZonedDateTime.parse("2011-09-09T01:48:08.888Z"), UUID.fromString("6b598c2a-0a95-4f3f-9298-5a4d70ca13fc"), 9999, "nested",
                ImmutableList.of("abc", "xyz"),
                new NestedDummyEventWithZonedDateTimeClass.NestedPart("first",
                             new NestedDummyEventWithZonedDateTimeClass.NestedPart("second",
                                new NestedDummyEventWithZonedDateTimeClass.NestedPart("third", null))),
                ImmutableList.of(new NestedDummyEventWithZonedDateTimeClass.NestedPart("listFirst",
                             new NestedDummyEventWithZonedDateTimeClass.NestedPart("listSecond", null)),
                                new NestedDummyEventWithZonedDateTimeClass.NestedPart("listThird", null)));

        assertEventJson(createEventGenerator(ImmutableList.of(nestedEvent)), "nested.json");
    }

    @Test(expectedExceptions = InvalidEventException.class, expectedExceptionsMessageRegExp = "Cycle detected in event data:.*")
    public void testCircularEvent()
            throws Exception
    {
        eventWriter.writeEvents(createEventGenerator(ImmutableList.of(new CircularEventClass())), nullOutputStream());
    }

    @Test(expectedExceptions = InvalidEventException.class, expectedExceptionsMessageRegExp = "Cycle detected in event data:.*")
    public void testChainedCircularEvent()
            throws Exception
    {
        ChainedPart a = new ChainedPart("a");
        ChainedPart b = new ChainedPart("b");
        ChainedPart c = new ChainedPart("c");
        a.setPart(b);
        b.setPart(c);
        c.setPart(a);

        ChainedCircularEventClass event = new ChainedCircularEventClass(a);

        eventWriter.writeEvents(createEventGenerator(ImmutableList.of(event)), nullOutputStream());
    }

    private void assertEventJson(EventClient.EventGenerator<?> events, String resource)
            throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        eventWriter.writeEvents(events, out);

        String json = out.toString(UTF_8);
        assertEquals(json, TestingUtils.getNormalizedJson(resource));
    }

    private static <T> EventClient.EventGenerator<T> createEventGenerator(final Iterable<T> events)
    {
        return eventPoster -> {
            for (T event : events) {
                eventPoster.post(event);
            }
        };
    }
}
