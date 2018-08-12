/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm.util;

import java.util.Arrays;

public class VMUtils {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static byte[] sha3(byte[] input) {
        return null;
    }

    public static byte[] calcNewAddr(byte[] address, long nonce) {
        return null;
    }

    public static byte[] stripLeadingZeroes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != 0) {
                return Arrays.copyOfRange(bytes, i, bytes.length);
            }
        }

        return new byte[0];
    }

    public static int compareUnsigned(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        int minLen = Math.min(a.length, b.length);
        for (int i = 0; i < minLen; ++i) {
            int aVal = a[i] & 0xFF, bVal = b[i] & 0xFF;
            if (aVal < bVal) {
                return -1;
            }
            if (aVal > bVal) {
                return 1;
            }
        }
        if (a.length < b.length) {
            return -1;
        }
        if (a.length > b.length) {
            return 1;
        }
        return 0;
    }

    public static String toHexString(byte[] raw) {
        char[] hex = new char[raw.length * 2];

        for (int i = 0; i < raw.length; i++) {
            hex[i * 2] = intToHex[(raw[i] & 0xF0) >> 4];
            hex[i * 2 + 1] = intToHex[raw[i] & 0x0F];
        }

        return new String(hex);
    }

    public static byte[] fromHexString(String hex) {
        byte[] raw = new byte[hex.length() / 2];

        char[] chars = hex.toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            raw[i / 2] = (byte) ((hexToInt[chars[i]] << 4) + hexToInt[chars[i + 1]]);
        }

        return raw;
    }

    private static final char[] intToHex = "0123456789abcdef".toCharArray();
    private static final int[] hexToInt = new int[128];

    static {
        for (byte i = 0; i < 16; i++) {
            if (i < 10) {
                hexToInt['0' + i] = i;
            } else {
                hexToInt['a' + i - 10] = i;
                hexToInt['A' + i - 10] = i;
            }
        }
    }

    private VMUtils() {
    }
}
