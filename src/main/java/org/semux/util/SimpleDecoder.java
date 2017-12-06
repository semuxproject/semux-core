/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.UnsupportedEncodingException;

import org.semux.util.exception.SimpleDecoderException;

public class SimpleDecoder {
    private static final String ENCODING = "UTF-8";

    private byte[] in;
    private int from;
    private int to;
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

    private void require(int n) {
        if (to - index < n) {
            String msg = String.format("input [%d, %d], require: [%d %d]", from, to, index, index + n);
            throw new IndexOutOfBoundsException(msg);
        }
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

    private long uintToLong(int i) {
        return i & 0x00000000ffffffffL;
    }

    public long readLong() {
        int i1 = readInt();
        int i2 = readInt();

        return (uintToLong(i1) << 32) | uintToLong(i2);
    }

    public byte[] readBytes() {
        int len = readInt();

        require(len);
        byte[] buf = new byte[len];
        System.arraycopy(in, index, buf, 0, len);
        index += len;

        return buf;
    }

    public String readString() {
        int len = readInt();

        require(len);
        byte[] buf = new byte[len];
        System.arraycopy(in, index, buf, 0, len);
        index += len;

        try {
            return new String(buf, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new SimpleDecoderException(e);
        }
    }

    public SimpleDecoder readRecursively() {
        int len = readInt();

        require(len);
        return new SimpleDecoder(in, index, index + len);
    }

    public int getReadIndex() {
        return index;
    }
}
