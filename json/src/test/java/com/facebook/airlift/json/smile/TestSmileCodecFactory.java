package com.facebook.airlift.json.smile;

import com.facebook.airlift.json.ImmutablePerson;
import com.facebook.airlift.json.Person;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class TestSmileCodecFactory
{
    private SmileCodecFactory codecFactory;

    @BeforeClass
    public void setUp()
    {
        codecFactory = new SmileCodecFactory();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown()
    {
        codecFactory = null;
    }

    @Test
    public void testSmileCodec()
    {
        SmileCodec<Person> smileCodec = codecFactory.smileCodec(Person.class);
        Person expected = new Person().setName("person-1").setRocks(true);
        byte[] smile = smileCodec.toSmile(expected);
        assertEquals(smileCodec.fromSmile(smile), expected);
    }

    @Test
    public void testListSmileCodec()
    {
        SmileCodec<List<Person>> smileCodec = codecFactory.listSmileCodec(Person.class);
        this.validatePersonListSmileCodec(smileCodec);
    }

    @Test
    public void testTypeTokenList()
    {
        SmileCodec<List<Person>> smileCodec = codecFactory.smileCodec(new TypeToken<List<Person>>() {}.getType());
        this.validatePersonListSmileCodec(smileCodec);
    }

    @Test
    public void testListNullValues()
    {
        SmileCodec<List<String>> smileCodec = codecFactory.listSmileCodec(String.class);

        List<String> list = new ArrayList<>();
        list.add(null);
        list.add("abc");

        assertEquals(smileCodec.fromSmile(smileCodec.toSmile(list)), list);
    }

    @Test
    public void testMapSmileCodec()
    {
        SmileCodec<Map<String, Person>> smileCodec = codecFactory.mapSmileCodec(String.class, Person.class);
        validatePersonMapSmileCodec(smileCodec);
    }

    @Test
    public void testMapSmileCodecFromSmileCodec()
    {
        SmileCodec<Map<String, Person>> smileCodec = codecFactory.mapSmileCodec(String.class, codecFactory.smileCodec(Person.class));
        validatePersonMapSmileCodec(smileCodec);
    }

    @Test
    public void testTypeLiteralMap()
    {
        SmileCodec<Map<String, Person>> smileCodec = codecFactory.smileCodec(new TypeToken<Map<String, Person>>() {}.getType());
        validatePersonMapSmileCodec(smileCodec);
    }

    @Test
    public void testMapNullValues()
    {
        SmileCodec<Map<String, String>> smileCodec = codecFactory.mapSmileCodec(String.class, String.class);

        Map<String, String> map = new HashMap<>();
        map.put("x", null);
        map.put("y", "abc");

        assertEquals(smileCodec.fromSmile(smileCodec.toSmile(map)), map);
    }

    @Test
    public void testImmutableSmileCodec()
    {
        SmileCodec<ImmutablePerson> smileCodec = codecFactory.smileCodec(ImmutablePerson.class);
        ImmutablePerson expected = new ImmutablePerson("person-1", true);
        assertEquals(smileCodec.fromSmile(smileCodec.toSmile(expected)), expected);
    }

    @Test
    public void testImmutableListSmileCodec()
    {
        SmileCodec<List<ImmutablePerson>> smileCodec = codecFactory.listSmileCodec(ImmutablePerson.class);
        validateImmutablePersonListSmileCodec(smileCodec);
    }

    @Test
    public void testImmutableListSmileCodecFromSmileCodec()
    {
        SmileCodec<List<ImmutablePerson>> smileCodec = codecFactory.listSmileCodec(codecFactory.smileCodec(ImmutablePerson.class));
        validateImmutablePersonListSmileCodec(smileCodec);
    }

    @Test
    public void testImmutableTypeTokenList()
    {
        SmileCodec<List<ImmutablePerson>> smileCodec = codecFactory.smileCodec(new TypeToken<List<ImmutablePerson>>() {}.getType());
        validateImmutablePersonListSmileCodec(smileCodec);
    }

    @Test
    public void testImmutableMapSmileCodec()
    {
        SmileCodec<Map<String, ImmutablePerson>> smileCodec = codecFactory.mapSmileCodec(String.class, ImmutablePerson.class);
        validateImmutablePersonMapSmileCodec(smileCodec);
    }

    @Test
    public void testImmutableMapSmileCodecFromSmileCodec()
    {
        SmileCodec<Map<String, ImmutablePerson>> smileCodec = codecFactory.mapSmileCodec(String.class, codecFactory.smileCodec(ImmutablePerson.class));
        validateImmutablePersonMapSmileCodec(smileCodec);
    }

    @Test
    public void testImmutableTypeTokenMap()
    {
        SmileCodec<Map<String, ImmutablePerson>> smileCodec = codecFactory.smileCodec(new TypeToken<Map<String, ImmutablePerson>>() {}.getType());
        validateImmutablePersonMapSmileCodec(smileCodec);
    }

    @Test
    public void testWriteBytes()
    {
        SmileCodec<ImmutablePerson> personCodec = codecFactory.smileCodec(ImmutablePerson.class);
        ImmutablePerson person = new ImmutablePerson("person-1", true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        personCodec.writeBytes(output, person);
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        assertEquals(personCodec.readBytes(input), person);
    }

    private void validatePersonListSmileCodec(SmileCodec<List<Person>> smileCodec)
    {
        List<Person> expected = ImmutableList.of(
                new Person().setName("person-1").setRocks(true),
                new Person().setName("person-2").setRocks(true),
                new Person().setName("person-3").setRocks(true));

        byte[] smileBytes = smileCodec.toSmile(expected);
        List<Person> actual = smileCodec.fromSmile(smileBytes);
        assertEquals(actual, expected);
    }

    private void validatePersonMapSmileCodec(SmileCodec<Map<String, Person>> smileCodec)
    {
        Map<String, Person> expected = ImmutableMap.<String, Person>builder()
                .put("person-1", new Person().setName("person-1").setRocks(true))
                .put("person-2", new Person().setName("person-2").setRocks(true))
                .put("person-3", new Person().setName("person-3").setRocks(true))
                .build();

        byte[] smileBytes = smileCodec.toSmile(expected);
        Map<String, Person> actual = smileCodec.fromSmile(smileBytes);
        assertEquals(actual, expected);
    }

    private void validateImmutablePersonMapSmileCodec(SmileCodec<Map<String, ImmutablePerson>> smileCodec)
    {
        Map<String, ImmutablePerson> expected = ImmutableMap.<String, ImmutablePerson>builder()
                .put("person-1", new ImmutablePerson("person-1", true))
                .put("person-2", new ImmutablePerson("person-2", true))
                .put("person-3", new ImmutablePerson("person-3", true))
                .build();

        byte[] smileBytes = smileCodec.toSmile(expected);
        Map<String, ImmutablePerson> actual = smileCodec.fromSmile(smileBytes);
        assertEquals(actual, expected);
    }

    private void validateImmutablePersonListSmileCodec(SmileCodec<List<ImmutablePerson>> smileCodec)
    {
        List<ImmutablePerson> expected = ImmutableList.of(
                new ImmutablePerson("person-1", true),
                new ImmutablePerson("person-2", true),
                new ImmutablePerson("person-3", true));

        byte[] smileBytes = smileCodec.toSmile(expected);
        List<ImmutablePerson> actual = smileCodec.fromSmile(smileBytes);
        assertEquals(actual, expected);
    }
}
