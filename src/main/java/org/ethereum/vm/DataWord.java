/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/*
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
package org.ethereum.vm;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.ethereum.vm.util.ByteArrayUtil;
import org.ethereum.vm.util.HexUtil;

/**
 * DataWord is the 32-byte array representation of a 256-bit number.
 *
 * NOTE: the underlying byte array is shared across dataword and should not be
 * modified anywhere
 */
public class DataWord implements Comparable<DataWord> {

    public static final BigInteger TWO_POW_256 = BigInteger.valueOf(2).pow(256);
    public static final BigInteger MAX_VALUE = TWO_POW_256.subtract(BigInteger.ONE);

    public static final DataWord ZERO = new DataWord(0);
    public static final DataWord ONE = new DataWord(1);

    public static final int SIZE = 32;

    private byte[] data = new byte[SIZE];

    public DataWord(int num) {
        this(ByteBuffer.allocate(4).putInt(num).array());
    }

    public DataWord(long num) {
        this(ByteBuffer.allocate(8).putLong(num).array());
    }

    public DataWord(BigInteger num) {
        this(num.toByteArray());
    }

    public DataWord(String hex) {
        this(HexUtil.fromHexString(hex));
    }

    public DataWord(byte[] data) {
        if (data.length == 32) {
            this.data = data; // shallow copy
        } else if (data.length < 32) {
            System.arraycopy(data, 0, this.data, 32 - data.length, data.length);
        } else {
            throw new RuntimeException("Data word can't exceed 32 bytes");
        }
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getLast20Bytes() {
        return Arrays.copyOfRange(data, data.length - 20, data.length);
    }

    public BigInteger value() {
        return new BigInteger(1, data);
    }

    public BigInteger sValue() {
        return new BigInteger(data);
    }

    /**
     * Converts this DataWord to an integer, checking for lost information. If this
     * DataWord is out of the possible range, then an ArithmeticException is thrown.
     *
     * @return an integer
     * @throws ArithmeticException
     *             if this value is larger than {@link Integer#MAX_VALUE}
     */
    public int intValue() throws ArithmeticException {
        return intValue(false);
    }

    /**
     * Returns {@link Integer#MAX_VALUE} in case of overflow
     */
    public int intValueSafe() {
        return intValue(true);
    }

    /**
     * Converts this DataWord to a long integer, checking for lost information. If
     * this DataWord is out of the possible range, then an ArithmeticException is
     * thrown.
     *
     * @return a long integer
     * @throws ArithmeticException
     *             if this value is larger than {@link Long#MAX_VALUE}
     */
    public long longValue() {
        return longValue(false);
    }

    /**
     * Returns {@link Long#MAX_VALUE} in case of overflow
     */
    public long longValueSafe() {
        return longValue(true);
    }

    public boolean isZero() {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public DataWord and(DataWord w2) {
        byte[] buffer = new byte[32];
        for (int i = 0; i < this.data.length; ++i) {
            buffer[i] = (byte) (this.data[i] & w2.data[i]);
        }
        return new DataWord(buffer);
    }

    public DataWord or(DataWord w2) {
        byte[] buffer = new byte[32];
        for (int i = 0; i < this.data.length; ++i) {
            buffer[i] = (byte) (this.data[i] | w2.data[i]);
        }
        return new DataWord(buffer);
    }

    public DataWord xor(DataWord w2) {
        byte[] buffer = new byte[32];
        for (int i = 0; i < this.data.length; ++i) {
            buffer[i] = (byte) (this.data[i] ^ w2.data[i]);
        }
        return new DataWord(buffer);
    }

    // bitwise not
    public DataWord bnot() {
        byte[] buffer = new byte[32];
        for (int i = 0; i < this.data.length; ++i) {
            buffer[i] = (byte) (~this.data[i]);
        }
        return new DataWord(buffer);
    }

    // Credit -> http://stackoverflow.com/a/24023466/459349
    public DataWord add(DataWord word) {
        byte[] result = new byte[32];
        for (int i = 31, overflow = 0; i >= 0; i--) {
            int v = (this.data[i] & 0xff) + (word.data[i] & 0xff) + overflow;
            result[i] = (byte) v;
            overflow = v >>> 8;
        }
        return new DataWord(result);
    }

    public DataWord mul(DataWord word) {
        BigInteger result = value().multiply(word.value());

        return new DataWord(result.and(MAX_VALUE));
    }

    public DataWord div(DataWord word) {
        if (word.isZero()) {
            return ZERO;
        } else {
            BigInteger result = value().divide(word.value());
            return new DataWord(result.and(MAX_VALUE));
        }
    }

    public DataWord sDiv(DataWord word) {
        if (word.isZero()) {
            return ZERO;
        } else {
            BigInteger result = sValue().divide(word.sValue());
            return new DataWord(result.and(MAX_VALUE));
        }
    }

    public DataWord sub(DataWord word) {
        BigInteger result = value().subtract(word.value());
        return new DataWord(result.and(MAX_VALUE));
    }

    public DataWord exp(DataWord word) {
        BigInteger result = value().modPow(word.value(), TWO_POW_256);
        return new DataWord(result);
    }

    public DataWord mod(DataWord word) {
        if (word.isZero()) {
            return ZERO;
        } else {
            BigInteger result = value().mod(word.value());
            return new DataWord(result.and(MAX_VALUE));
        }
    }

    public DataWord sMod(DataWord word) {
        if (word.isZero()) {
            return ZERO;
        } else {
            BigInteger result = sValue().abs().mod(word.sValue().abs());
            result = (sValue().signum() == -1) ? result.negate() : result;

            return new DataWord(result.and(MAX_VALUE));
        }
    }

    public DataWord addmod(DataWord word1, DataWord word2) {
        if (word2.isZero()) {
            return ZERO;
        } else {
            BigInteger result = value().add(word1.value()).mod(word2.value());
            return new DataWord(result.and(MAX_VALUE));
        }
    }

    public DataWord mulmod(DataWord word1, DataWord word2) {
        if (this.isZero() || word1.isZero() || word2.isZero()) {
            return ZERO;
        } else {
            BigInteger result = value().multiply(word1.value()).mod(word2.value());
            return new DataWord(result.and(MAX_VALUE));
        }
    }

    public DataWord signExtend(byte k) {
        if (0 > k || k > 31) {
            throw new IndexOutOfBoundsException();
        }

        byte[] buffer = data.clone();
        byte mask = this.sValue().testBit((k * 8) + 7) ? (byte) 0xff : 0;
        for (int i = 31; i > k; i--) {
            buffer[31 - i] = mask;
        }

        return new DataWord(buffer);
    }

    public int bytesOccupied() {
        for (int i = 0; i < 32; i++) {
            if (data[i] != 0) {
                return 32 - i;
            }
        }

        return 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DataWord && Arrays.equals(this.data, ((DataWord) o).data);
    }

    @Override
    public int compareTo(DataWord o) {
        return ByteArrayUtil.compareUnsigned(this.data, o.data);
    }

    @Override
    public String toString() {
        return HexUtil.toHexString(data);
    }

    private int intValue(boolean safe) {
        if (bytesOccupied() > 4 || (data[SIZE - 4] & 0x80) != 0) {
            if (safe) {
                return Integer.MAX_VALUE;
            } else {
                throw new ArithmeticException();
            }
        }

        int value = 0;
        for (int i = 0; i < 4; i++) {
            value = (value << 8) + (0xff & data[SIZE - 4 + i]);
        }

        return value;
    }

    private long longValue(boolean safe) {
        if (bytesOccupied() > 8 || (data[SIZE - 8] & 0x80) != 0) {
            if (safe) {
                return Long.MAX_VALUE;
            } else {
                throw new ArithmeticException();
            }
        }

        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) + (0xff & data[SIZE - 8 + i]);
        }

        return value;
    }
}
