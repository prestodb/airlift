package com.facebook.airlift.json.smile;

import com.facebook.airlift.json.Codec;
import com.facebook.airlift.json.ImmutablePerson;
import com.facebook.airlift.json.JsonCodecFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.facebook.airlift.json.smile.CodecUtils.deserializeCompressed;
import static com.facebook.airlift.json.smile.CodecUtils.serializeCompressed;
import static org.testng.Assert.assertEquals;

public class TestCodecUtils
{
    private JsonCodecFactory jsonCodecFactory;
    private SmileCodecFactory smileCodecFactory;

    @BeforeClass
    public void setUp()
    {
        jsonCodecFactory = new JsonCodecFactory();
        smileCodecFactory = new SmileCodecFactory();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown()
    {
        jsonCodecFactory = null;
        smileCodecFactory = null;
    }

    @Test
    public void testSerializeCompressed()
    {
        ImmutablePerson person = new ImmutablePerson("person-1", true);
        Codec<ImmutablePerson> jsonCodec = jsonCodecFactory.jsonCodec(ImmutablePerson.class);
        assertEquals(deserializeCompressed(jsonCodec, serializeCompressed(jsonCodec, person)), person);
        Codec<ImmutablePerson> smileCodec = smileCodecFactory.smileCodec(ImmutablePerson.class);
        assertEquals(deserializeCompressed(smileCodec, serializeCompressed(smileCodec, person)), person);
    }
}
