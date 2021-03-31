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
package com.facebook.airlift.json.smile;

import com.facebook.airlift.json.Person;
import com.facebook.airlift.json.TestJsonModule.Car;
import com.facebook.airlift.json.TestJsonModule.JsonValueAndStaticFactoryMethod;
import com.facebook.airlift.json.TestJsonModule.NoJsonPropertiesInJsonCreator;
import com.facebook.airlift.json.TestJsonModule.SuperDuperNameList;
import com.facebook.airlift.json.TestJsonModule.SuperDuperNameListDeserializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.joda.time.format.ISODateTimeFormat;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.facebook.airlift.json.JsonBinder.jsonBinder;
import static com.facebook.airlift.json.TestJsonModule.CAR;
import static org.testng.Assert.assertEquals;

public class TestSmileSupport
{
    private ObjectMapper objectMapper;

    @BeforeClass
    public void setUp()
    {
        Injector injector = Guice.createInjector(new SmileModule(),
                binder -> {
                    jsonBinder(binder).addSerializerBinding(SuperDuperNameList.class).toInstance(ToStringSerializer.instance);
                    jsonBinder(binder).addDeserializerBinding(SuperDuperNameList.class).to(SuperDuperNameListDeserializer.class);
                });
        objectMapper = injector.getInstance(Key.get(ObjectMapper.class, ForSmile.class));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown()
    {
        objectMapper = null;
    }

    @Test
    public void testSmileCodecFactoryBinding()
    {
        Injector injector = Guice.createInjector(new SmileModule());
        SmileCodecFactory codecFactory = injector.getInstance(SmileCodecFactory.class);
        SmileCodec<Person> personJsonCodec = codecFactory.smileCodec(Person.class);
        Person person = new Person();
        person.setName("name");
        person.setLastName(Optional.of("lastname"));
        Person actual = personJsonCodec.fromSmile(personJsonCodec.toSmile(person));
        assertEquals(actual, person);
    }

    @Test
    public void testObjectMapper()
            throws IOException
    {
        byte[] bytes = objectMapper.writeValueAsBytes(CAR);
        Car car = objectMapper.readValue(bytes, CAR.getClass());
        assertEquals(car, CAR);
    }

    @Test
    public void testFieldDetection()
            throws Exception
    {
        Map<String, Object> actual = createCarMap();

        // notes is not annotated so should not be included
        // color is null so should not be included
        assertEquals(actual.keySet(), ImmutableSet.of("make", "model", "year", "purchased", "nameList"));
    }

    @Test
    public void testDateTimeRendered()
            throws Exception
    {
        Map<String, Object> actual = createCarMap();

        assertEquals(actual.get("purchased"), ISODateTimeFormat.dateTime().print(CAR.getPurchased()));
    }

    @Test
    public void testGuavaRoundTrip()
            throws Exception
    {
        List<Integer> list = ImmutableList.of(3, 5, 8);

        byte[] json = objectMapper.writeValueAsBytes(list);
        List<Integer> actual = objectMapper.readValue(json, new TypeReference<ImmutableList<Integer>>() {});

        assertEquals(actual, list);
    }

    @Test
    public void testIgnoreUnknownFields()
            throws Exception
    {
        Map<String, Object> data = new HashMap<>(createCarMap());

        // add an unknown field
        data.put("unknown", "bogus");

        // Jackson should deserialize the object correctly with the extra unknown data
        assertEquals(objectMapper.readValue(objectMapper.writeValueAsBytes(data), Car.class), CAR);
    }

    @Test
    public void testPropertyNamesFromParameterNames()
            throws Exception
    {
        NoJsonPropertiesInJsonCreator value = new NoJsonPropertiesInJsonCreator("first value", "second value");
        NoJsonPropertiesInJsonCreator mapped = objectMapper.readValue(objectMapper.writeValueAsBytes(value), NoJsonPropertiesInJsonCreator.class);
        assertEquals(mapped.getFirst(), "first value");
        assertEquals(mapped.getSecond(), "second value");
    }

    @Test
    public void testJsonValueAndStaticFactoryMethod()
            throws Exception
    {
        JsonValueAndStaticFactoryMethod value = JsonValueAndStaticFactoryMethod.valueOf("some value");
        JsonValueAndStaticFactoryMethod mapped = objectMapper.readValue(objectMapper.writeValueAsBytes(value), JsonValueAndStaticFactoryMethod.class);
        assertEquals(mapped.getValue(), "some value");
    }

    private Map<String, Object> createCarMap()
            throws IOException
    {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(CAR), new TypeReference<Map<String, Object>>() {});
    }
}
