/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.semux.core.Amount.Unit.NANO_SEM;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.semux.core.Amount;
import org.semux.util.exception.SimpleCodecException;

public class SimpleDecoder {
    private static final String ENCODING = "UTF-8";

    private final byte[] in;
    private final int from;
    private final int to;

    private int index;

    public SimpleDecoder(byte[] in) {
        this(in, 0, in.length);
    }

    public SimpleDecoder(byte[] in, int from) {
        this(in, from, in.length);
    }

    public SimpleDecoder(byte[] in, int from, int to) {
        this.in = in;
        this.from = from;
        this.to = to;
        this.index = from;
    }

    public boolean readBoolean() {
        require(1);
        return in[index++] != 0;
    }

    public byte readByte() {
        require(1);
        return in[index++];
    }

    public short readShort() {
        require(2);
        return (short) ((in[index++] & 0xFF) << 8 | (in[index++] & 0xFF));
    }

    public int readInt() {
        require(4);
        return in[index++] << 24 | (in[index++] & 0xFF) << 16 | (in[index++] & 0xFF) << 8 | (in[index++] & 0xFF);
    }

    public long readLong() {
        int i1 = readInt();
        int i2 = readInt();

        return (unsignedInt(i1) << 32) | unsignedInt(i2);
    }

    public Amount readAmount() {
        return NANO_SEM.of(readLong());
    }

    /**
     * Decode a byte array.
     *
     * @param vlq
     *            should always be true unless we're providing pre-mainnet support.
     */
    public byte[] readBytes(boolean vlq) {
        int len = vlq ? readSize() : readInt();

        require(len);
        byte[] buf = new byte[len];
        System.arraycopy(in, index, buf, 0, len);
        index += len;

        return buf;
    }

    public byte[] readBytes() {
        return readBytes(true);
    }

    public String readString() {
        try {
            return new String(readBytes(), ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new SimpleCodecException(e);
        }
    }

    public int getReadIndex() {
        return index;
    }

    /**
     * Reads size from the input.
     * 
     * @return
     */
    protected int readSize() {
        int size = 0;
        for (int i = 0; i < 4; i++) {
            require(1);
            byte b = in[index++];

            size = (size << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return size;
    }

    /**
     * Checks if the required bytes is satisfied.
     * 
     * @param n
     */
    protected void require(int n) {
        if (to - index < n) {
            String msg = String.format("input [%d, %d], require: [%d %d]", from, to, index, index + n);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    /**
     * Re-interprets an integer as unsigned integer.
     * 
     * @param i
     *            an integer
     * @return the unsigned value, represented in long
     */
    protected long unsignedInt(int i) {
        return i & 0x00000000ffffffffL;
    }

    /**
     * Reads a byte[] -> byte[] map
     * @return map
     */
    public Map<ByteArray, byte[]> readByteMap() {
        int numEntries = readSize();
        Map<ByteArray, byte[]> ret = new HashMap<>();
        for(int i = 0; i< numEntries; i++) {
            byte[] key = readBytes();
            byte[] value = readBytes();
            ret.put(ByteArray.of(key), value);
        }

        return ret;
    }
}
