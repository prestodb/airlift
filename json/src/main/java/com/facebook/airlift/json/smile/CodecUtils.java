package com.facebook.airlift.json.smile;

import com.facebook.airlift.json.Codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static java.util.zip.Deflater.BEST_COMPRESSION;

public class CodecUtils
{
    private CodecUtils() {}

    public static <T> byte[] serializeCompressed(Codec<T> codec, T instance)
    {
        ByteArrayOutputStream rawOutput = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        deflater.setLevel(BEST_COMPRESSION);
        try (DeflaterOutputStream deflateOutput = new DeflaterOutputStream(rawOutput, deflater)) {
            codec.writeBytes(deflateOutput, instance);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rawOutput.toByteArray();
    }

    public static <T> T deserializeCompressed(Codec<T> codec, byte[] bytes)
    {
        try (InflaterInputStream inflateInput = new InflaterInputStream(new ByteArrayInputStream(bytes))) {
            return codec.readBytes(inflateInput);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
