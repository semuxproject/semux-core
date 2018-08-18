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

import java.util.Random;

public class ByteArrayUtil {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

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

    private static Random r = new Random();

    public static byte[] random(int n) {
        byte[] bytes = new byte[n];
        r.nextBytes(bytes);
        return bytes;
    }
}
