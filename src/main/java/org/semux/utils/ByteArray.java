/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.utils;

import java.util.Arrays;

import org.semux.crypto.Hex;

public class ByteArray {
    private final byte[] data;
    private final int hash;

    public ByteArray(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data can not be null");
        }
        this.data = data;
        this.hash = Arrays.hashCode(data);
    }

    public static ByteArray of(byte[] data) {
        return new ByteArray(data);
    }

    public boolean equals(Object other) {
        if (other instanceof ByteArray) {
            return Arrays.equals(data, ((ByteArray) other).data);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public int length() {
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return Hex.encode(data);
    }
}