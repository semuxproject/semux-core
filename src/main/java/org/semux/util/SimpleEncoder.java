/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.semux.util.exception.SimpleEncoderException;

public class SimpleEncoder {
    private ByteArrayOutputStream out;

    public SimpleEncoder() {
        this.out = new ByteArrayOutputStream();
    }

    public SimpleEncoder(byte[] toAppend) {
        this();
        try {
            out.write(toAppend);
        } catch (IOException e) {
            throw new SimpleEncoderException(e);
        }
    }

    public void writeBoolean(boolean b) {
        out.write(b ? 1 : 0);
    }

    public void writeByte(byte b) {
        out.write(b);
    }

    public void writeShort(short s) {
        out.write(0xFF & (s >>> 8));
        out.write(0xFF & s);
    }

    public void writeInt(int i) {
        out.write(0xFF & (i >>> 24));
        out.write(0xFF & (i >>> 16));
        out.write(0xFF & (i >>> 8));
        out.write(0xFF & i);
    }

    public void writeLong(long l) {
        int i1 = (int) (l >>> 32);
        int i2 = (int) l;

        writeInt(i1);
        writeInt(i2);
    }

    public void writeBytes(byte[] bytes) {
        writeInt(bytes.length);
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new SimpleEncoderException(e);
        }
    }

    public void writeString(String s) {
        writeInt(s.length());
        try {
            out.write(Bytes.of(s));
        } catch (IOException e) {
            throw new SimpleEncoderException(e);
        }
    }

    public void writeRecursively(SimpleEncoder se) {
        byte[] bytes = se.toBytes();
        writeInt(bytes.length);
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new SimpleEncoderException(e);
        }
    }

    public byte[] toBytes() {
        return out.toByteArray();
    }

    public int getWriteIndex() {
        return out.size();
    }
}
