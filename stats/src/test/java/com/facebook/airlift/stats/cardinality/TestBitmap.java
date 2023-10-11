package com.facebook.airlift.stats.cardinality;

import org.openjdk.jol.info.ClassLayout;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestBitmap
{
    private TestBitmap() {}

    @Test
    public static void testRoundTrip()
    {
        byte[] bytes = randomBytes(100);
        assertEquals(Bitmap.fromBytes(bytes).toBytes(), bytes);
    }

    @Test
    public static void testSetBit()
    {
        Bitmap bitmap = new Bitmap(24);

        // This should create the following bitmap:
        // 00000011_00000101_01010101
        bitmap.setBit(0, true);
        bitmap.setBit(1, true);
        bitmap.setBit(8, true);
        bitmap.setBit(10, true);
        bitmap.setBit(16, true);
        bitmap.setBit(18, true);
        bitmap.setBit(20, true);
        bitmap.setBit(22, true);

        byte[] bytes = bitmap.toBytes();
        assertEquals(bytes[0], 0b00000011);
        assertEquals(bytes[1], 0b00000101);
        assertEquals(bytes[2], 0b01010101);

        // Now clear all bits
        for (int i = 0; i < 24; i++) {
            bitmap.setBit(i, false);
        }

        bytes = bitmap.toBytes();
        assertEquals(bytes[0], 0);
        assertEquals(bytes[1], 0);
        assertEquals(bytes[2], 0);
    }

    @Test
    public static void testGetBit()
    {
        Bitmap bitmap = new Bitmap(4096);

        for (int i = 0; i < 4096; i++) {
            bitmap.setBit(i, true);
            assertTrue(bitmap.getBit(i));
            bitmap.setBit(i, false);
            assertFalse(bitmap.getBit(i));
        }
    }

    @Test
    public void testGetBitCount()
    {
        int length = 1024;
        Bitmap bitmap = new Bitmap(length);
        assertEquals(bitmap.getBitCount(), 0); // all zeros at initialization
        for (int i = 0; i < length; i++) {
            bitmap.setBit(i, true);
            assertEquals(bitmap.getBitCount(), i + 1); // i + 1 "true" bits
        }
    }

    @Test
    public static void testFlipBit()
    {
        Bitmap bitmap = new Bitmap(4096);

        for (int i = 0; i < 4096; i++) {
            bitmap.flipBit(i);
            assertTrue(bitmap.getBit(i));
            bitmap.flipBit(i);
            assertFalse(bitmap.getBit(i));
            bitmap.flipBit(i);
            assertTrue(bitmap.getBit(i));
        }
    }

    @Test
    public static void testLength()
    {
        for (int i = 1; i <= 10; i++) {
            Bitmap bitmap = new Bitmap(i * 8);
            assertEquals(bitmap.length(), i * 8);
            assertEquals(bitmap.byteLength(), i);
        }
    }

    @Test
    public static void testRandomFlips()
    {
        Bitmap bitmap = new Bitmap(16);

        // Note: TestingDeterministicRandomizationStrategy flips deterministically if and only if probability >= 0.5.

        TestingDeterministicRandomizationStrategy randomizationStrategy = new TestingDeterministicRandomizationStrategy();
        bitmap.flipBit(0, 0.75, randomizationStrategy);
        assertTrue(bitmap.getBit(0));
        bitmap.flipBit(0, 0.75, randomizationStrategy);
        assertFalse(bitmap.getBit(0));
        bitmap.flipBit(0, 0.25, randomizationStrategy);
        assertFalse(bitmap.getBit(0));

        bitmap.flipAll(0.75, randomizationStrategy);
        for (int i = 0; i < 16; i++) {
            assertTrue(bitmap.getBit(i));
        }

        bitmap.flipAll(0.25, randomizationStrategy);
        for (int i = 0; i < 16; i++) {
            assertTrue(bitmap.getBit(i));
        }
    }

    @Test
    public static void testClone()
    {
        Bitmap bitmapA = Bitmap.fromBytes(randomBytes(100));
        Bitmap bitmapB = bitmapA.clone();

        // all bits should match
        for (int i = 0; i < 100 * 8; i++) {
            assertEquals(bitmapA.getBit(i), bitmapB.getBit(i));
        }

        // but the bitmaps should point to different bits
        bitmapA.flipBit(0);
        assertEquals(bitmapA.getBit(0), !bitmapB.getBit(0));
    }

    @Test
    public static void testOr()
    {
        Bitmap bitmapA = Bitmap.fromBytes(randomBytes(100));
        Bitmap bitmapB = Bitmap.fromBytes(randomBytes(100));
        Bitmap bitmapC = bitmapA.or(bitmapB);

        for (int i = 0; i < 100 * 8; i++) {
            assertEquals(bitmapC.getBit(i), bitmapA.getBit(i) | bitmapB.getBit(i));
        }
    }

    @Test
    public static void testXor()
    {
        Bitmap bitmapA = Bitmap.fromBytes(randomBytes(100));
        Bitmap bitmapB = Bitmap.fromBytes(randomBytes(100));
        Bitmap bitmapC = bitmapA.xor(bitmapB);

        for (int i = 0; i < 100 * 8; i++) {
            assertEquals(bitmapC.getBit(i), bitmapA.getBit(i) ^ bitmapB.getBit(i));
        }
    }

    @Test
    public static void testRetainedSize()
    {
        Bitmap bitmap = Bitmap.fromBytes(randomBytes(100));
        assertEquals(bitmap.getRetainedSizeInBytes(), 100 + 16 + ClassLayout.parseClass(Bitmap.class).instanceSize());
    }

    private static byte[] randomBytes(int length)
    {
        byte[] bytes = new byte[length];
        Random random = new Random();
        random.nextBytes(bytes);
        return bytes;
    }
}
