package com.facebook.airlift.stats.cardinality;

import com.google.common.annotations.VisibleForTesting;
import io.airlift.slice.SizeOf;
import io.airlift.slice.SliceInput;
import org.openjdk.jol.info.ClassLayout;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A level of abstraction over the bitmaps used in sketches such as LPCA and SFM.
 * These are essentially arrays of booleans that support flipping and applying randomized response.
 * Concretely, these are stored as byte arrays.
 */
public class Bitmap
{
    private static final int INSTANCE_SIZE = ClassLayout.parseClass(Bitmap.class).instanceSize();

    private final byte[] bitmap;

    public Bitmap(int length)
    {
        validateLength(length);
        bitmap = new byte[length / Byte.SIZE];
    }

    private Bitmap(byte[] bytes)
    {
        bitmap = bytes;
    }

    public static Bitmap fromBytes(byte[] bytes)
    {
        return new Bitmap(bytes);
    }

    public static Bitmap fromSliceInput(SliceInput input, int length)
    {
        validateLength(length);
        byte[] bytes = new byte[length / Byte.SIZE];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = input.readByte();
        }
        return Bitmap.fromBytes(bytes);
    }

    public byte[] toBytes()
    {
        return bitmap;
    }

    @VisibleForTesting
    static int bitmapBitShift(int position)
    {
        return position % Byte.SIZE;
    }

    @VisibleForTesting
    static int bitmapByteIndex(int position)
    {
        // n.b.: position is 0-indexed
        return Math.floorDiv(position, Byte.SIZE);
    }

    public int byteLength()
    {
        return bitmap.length;
    }

    @Override
    public Bitmap clone()
    {
        return Bitmap.fromBytes(bitmap.clone());
    }

    public long getRetainedSizeInBytes()
    {
        return INSTANCE_SIZE + SizeOf.sizeOf(bitmap);
    }

    public boolean getBit(int position)
    {
        int b = bitmapByteIndex(position);
        int shift = bitmapBitShift(position);

        return ((bitmap[b] >> shift) & 1) == 1;
    }

    /**
     * The number of 1-bits in the bitmap
     */
    public int getBitCount()
    {
        int count = 0;
        for (byte b : bitmap) {
            count += Integer.bitCount(Byte.toUnsignedInt(b));
        }
        return count;
    }

    /**
     * Randomly (and independently) flip all bits with specified probability
     */
    public void flipAll(double probability, RandomizationStrategy randomizationStrategy)
    {
        for (int i = 0; i < bitmap.length * Byte.SIZE; i++) {
            flipBit(i, probability, randomizationStrategy);
        }
    }

    /**
     * Deterministically flips the bit at a given position
     */
    public void flipBit(int position)
    {
        byte oneBit = (byte) (1 << bitmapBitShift(position));
        bitmap[bitmapByteIndex(position)] ^= oneBit;
    }

    /**
     * Randomly flips the bit at a given position with specified probability
     */
    public void flipBit(int position, double probability, RandomizationStrategy randomizationStrategy)
    {
        if (randomizationStrategy.nextBoolean(probability)) {
            flipBit(position);
        }
    }

    public int length()
    {
        return bitmap.length * Byte.SIZE;
    }

    /**
     * Explicitly set the value of the bit at a given position
     */
    public void setBit(int position, boolean value)
    {
        byte oneBit = (byte) (1 << bitmapBitShift(position));
        if (value) {
            bitmap[bitmapByteIndex(position)] |= oneBit;
        }
        else {
            bitmap[bitmapByteIndex(position)] &= ~oneBit;
        }
    }

    public Bitmap or(Bitmap other)
    {
        byte[] bytes = toBytes().clone();
        byte[] bytesOther = other.toBytes();

        checkArgument(bytes.length == bytesOther.length, "cannot OR two bitmaps of different size");

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] |= bytesOther[i];
        }

        return Bitmap.fromBytes(bytes);
    }

    public Bitmap xor(Bitmap other)
    {
        byte[] bytes = toBytes().clone();
        byte[] bytesOther = other.toBytes();

        checkArgument(bytes.length == bytesOther.length, "cannot XOR two bitmaps of different size");

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= bytesOther[i];
        }

        return Bitmap.fromBytes(bytes);
    }

    private static void validateLength(int length)
    {
        checkArgument(length > 0 && length % Byte.SIZE == 0, "bitmap size must be a positive multiple of %s", Byte.SIZE);
    }
}
