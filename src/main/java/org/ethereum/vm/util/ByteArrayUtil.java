/*
 * Copyright (c) [2018] [ The Semux Developers ]
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm.util;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;

import java.math.BigInteger;

public class ByteArrayUtil {
    public static int firstNonZeroByte(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            if (data[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    public static byte[] stripLeadingZeroes(byte[] data) {

        if (data == null)
            return null;

        final int firstNonZero = firstNonZeroByte(data);
        switch (firstNonZero) {
        case -1:
            return EMPTY_BYTE_ARRAY;

        case 0:
            return data;

        default:
            byte[] result = new byte[data.length - firstNonZero];
            System.arraycopy(data, firstNonZero, result, 0, data.length - firstNonZero);

            return result;
        }
    }

    /**
     * Returns a number of zero bits preceding the highest-order ("leftmost")
     * one-bit interpreting input array as a big-endian integer value
     */
    public static int numberOfLeadingZeros(byte[] bytes) {

        int i = firstNonZeroByte(bytes);

        if (i == -1) {
            return bytes.length * 8;
        } else {
            int byteLeadingZeros = Integer.numberOfLeadingZeros((int) bytes[i] & 0xff) - 24;
            return i * 8 + byteLeadingZeros;
        }
    }

    /**
     * Parses fixed number of bytes starting from {@code offset} in {@code input}
     * array. If {@code input} has not enough bytes return array will be right
     * padded with zero bytes. I.e. if {@code offset} is higher than
     * {@code input.length} then zero byte array of length {@code len} will be
     * returned
     */
    public static byte[] parseBytes(byte[] input, int offset, int len) {
        if (offset >= input.length || len == 0)
            return EMPTY_BYTE_ARRAY;

        byte[] bytes = new byte[len];
        System.arraycopy(input, offset, bytes, 0, Math.min(input.length - offset, len));
        return bytes;
    }

    /**
     * Parses 32-bytes word from given input. Uses
     * {@link #parseBytes(byte[], int, int)} method, thus, result will be
     * right-padded with zero bytes if there is not enough bytes in {@code input}
     *
     * @param idx
     *            an index of the word starting from {@code 0}
     */
    public static byte[] parseWord(byte[] input, int idx) {
        return parseBytes(input, 32 * idx, 32);
    }

    /**
     * Parses 32-bytes word from given input. Uses
     * {@link #parseBytes(byte[], int, int)} method, thus, result will be
     * right-padded with zero bytes if there is not enough bytes in {@code input}
     *
     * @param idx
     *            an index of the word starting from {@code 0}
     * @param offset
     *            an offset in {@code input} array to start parsing from
     */
    public static byte[] parseWord(byte[] input, int offset, int idx) {
        return parseBytes(input, offset + 32 * idx, 32);
    }

    /**
     * Cast hex encoded value from byte[] to BigInteger null is parsed like byte[0]
     *
     * @param bb
     *            byte array contains the values
     * @return unsigned positive BigInteger value.
     */
    public static BigInteger bytesToBigInteger(byte[] bb) {
        return (bb == null || bb.length == 0) ? BigInteger.ZERO : new BigInteger(1, bb);
    }
}
