package org.semux.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class SimpleCodecTest {

    @Test
    public void testCodec() {
        boolean boolean1 = true;
        boolean boolean2 = false;
        byte byte1 = Byte.MAX_VALUE;
        byte byte2 = Byte.MIN_VALUE;
        short short1 = Short.MAX_VALUE;
        short short2 = Short.MIN_VALUE;
        int int1 = Integer.MAX_VALUE;
        int int2 = Integer.MIN_VALUE;
        long long1 = Long.MAX_VALUE;
        long long2 = Long.MIN_VALUE;
        byte[] bytes1 = Bytes.random(20);
        byte[] bytes2 = Bytes.EMPY_BYTES;
        String string1 = "test";
        String string2 = "";

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBoolean(boolean1);
        enc.writeByte(byte1);
        enc.writeShort(short1);
        enc.writeInt(int1);
        enc.writeLong(long1);
        enc.writeBytes(bytes1);
        enc.writeString(string1);
        enc.writeBoolean(boolean2);
        enc.writeByte(byte2);
        enc.writeShort(short2);
        enc.writeInt(int2);
        enc.writeLong(long2);
        enc.writeBytes(bytes2);
        enc.writeString(string2);
        byte[] encoded = enc.toBytes();

        SimpleDecoder dec = new SimpleDecoder(encoded);
        assertEquals(boolean1, dec.readBoolean());
        assertEquals(byte1, dec.readByte());
        assertEquals(short1, dec.readShort());
        assertEquals(int1, dec.readInt());
        assertEquals(long1, dec.readLong());
        assertArrayEquals(bytes1, dec.readBytes());
        assertEquals(string1, dec.readString());
        assertEquals(boolean2, dec.readBoolean());
        assertEquals(byte2, dec.readByte());
        assertEquals(short2, dec.readShort());
        assertEquals(int2, dec.readInt());
        assertEquals(long2, dec.readLong());
        assertArrayEquals(bytes2, dec.readBytes());
        assertEquals(string2, dec.readString());
    }
}
