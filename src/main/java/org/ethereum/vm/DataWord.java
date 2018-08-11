/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.semux.crypto.Hex;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

/**
 * DataWord is the 32-byte array representation of a 256-bit number.
 *
 * NOTE: the underlying byte array should be shared across dataword; clients are
 * not supposed to modify it.
 */
public class DataWord implements Comparable<DataWord> {

    public static final BigInteger TWO_256 = BigInteger.valueOf(2).pow(256);
    public static final BigInteger MAX_VALUE = TWO_256.subtract(BigInteger.ONE);
    public static final DataWord ZERO = new DataWord();

    public static final long MEM_SIZE = 32 + 16 + 16;

    private byte[] data = new byte[32];

    public DataWord() {
    }

    public DataWord(int num) {
        this(Bytes.of(num));
    }

    public DataWord(long num) {
        this(Bytes.of(num));
    }

    public DataWord(BigInteger num) {
        this(num.toByteArray());
    }

    private DataWord(ByteBuffer buffer) {
        this(buffer.array());
    }

    public DataWord(String data) {
        this(Hex.decode(data));
    }

    public DataWord(ByteArray wrappedData) {
        this(wrappedData.getData());
    }

    public DataWord(byte[] data) {
        if (data == null) {
            // TODO: test null input data
            // do nothing
        } else if (data.length == 32) {
            this.data = data;
        } else if (data.length <= 32) {
            System.arraycopy(data, 0, this.data, 32 - data.length, data.length);
        } else {
            throw new RuntimeException("Data word can't exceed 32 bytes: length = " + data.length);
        }
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNoLeadZeroesData() {
        return Bytes.stripLeadingZeroes(data);
    }

    public byte[] getLast20Bytes() {
        return Arrays.copyOfRange(data, 12, data.length);
    }

    public BigInteger value() {
        return new BigInteger(1, data);
    }

    /**
     * Converts this DataWord to an int, checking for lost information. If this
     * DataWord is out of the possible range for an int result then an
     * ArithmeticException is thrown.
     *
     * @return this DataWord converted to an int.
     * @throws ArithmeticException
     *             - if this will not fit in an int.
     */
    public int intValue() {
        int intVal = 0;
        for (byte aData : data) {
            intVal = (intVal << 8) + (aData & 0xff);
        }
        return intVal;
    }

    /**
     * In case of int overflow returns Integer.MAX_VALUE otherwise works as
     * #intValue()
     */
    public int intValueSafe() {
        int bytesOccupied = bytesOccupied();
        int intValue = intValue();
        if (bytesOccupied > 4 || intValue < 0) {
            return Integer.MAX_VALUE;
        }
        return intValue;
    }

    /**
     * Converts this DataWord to a long, checking for lost information. If this
     * DataWord is out of the possible range for a long result then an
     * ArithmeticException is thrown.
     *
     * @return this DataWord converted to a long.
     * @throws ArithmeticException
     *             - if this will not fit in a long.
     */
    public long longValue() {

        long longVal = 0;
        for (byte aData : data) {
            longVal = (longVal << 8) + (aData & 0xff);
        }

        return longVal;
    }

    /**
     * In case of long overflow returns Long.MAX_VALUE otherwise works as
     * #longValue()
     */
    public long longValueSafe() {
        int bytesOccupied = bytesOccupied();
        long longValue = longValue();
        if (bytesOccupied > 8 || longValue < 0) {
            return Long.MAX_VALUE;
        }
        return longValue;
    }

    public BigInteger sValue() {
        return new BigInteger(data);
    }

    public String bigIntValue() {
        return new BigInteger(data).toString();
    }

    public boolean isZero() {
        for (byte tmp : data) {
            if (tmp != 0) {
                return false;
            }
        }
        return true;
    }

    public DataWord and(DataWord w2) {
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] &= w2.data[i];
        }
        return this;
    }

    public DataWord or(DataWord w2) {
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] |= w2.data[i];
        }
        return this;
    }

    public DataWord xor(DataWord w2) {
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] ^= w2.data[i];
        }
        return this;
    }

    public void negate() {
        if (this.isZero())
            return;

        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] = (byte) ~this.data[i];
        }

        for (int i = this.data.length - 1; i >= 0; --i) {
            this.data[i] = (byte) (1 + this.data[i] & 0xFF);
            if (this.data[i] != 0) {
                break;
            }
        }
    }

    public void bnot() {
        if (this.isZero()) {
            this.data = bigIntToBytes32(MAX_VALUE);
        } else {
            this.data = bigIntToBytes32(MAX_VALUE.subtract(this.value()));
        }
    }

    // http://stackoverflow.com/a/24023466/459349
    public void add(DataWord word) {
        byte[] result = new byte[32];
        for (int i = 31, overflow = 0; i >= 0; i--) {
            int v = (this.data[i] & 0xff) + (word.data[i] & 0xff) + overflow;
            result[i] = (byte) v;
            overflow = v >>> 8;
        }
        this.data = result;
    }

    // old add-method with BigInteger quick hack
    public void add2(DataWord word) {
        BigInteger result = value().add(word.value());
        this.data = bigIntToBytes32(result.and(MAX_VALUE));
    }

    public void mul(DataWord word) {
        BigInteger result = value().multiply(word.value());
        this.data = bigIntToBytes32(result.and(MAX_VALUE));
    }

    public void div(DataWord word) {
        if (word.isZero()) {
            this.and(ZERO);
        } else {
            BigInteger result = value().divide(word.value());
            this.data = bigIntToBytes32(result.and(MAX_VALUE));
        }
    }

    public void sDiv(DataWord word) {
        if (word.isZero()) {
            this.and(ZERO);
        } else {
            BigInteger result = sValue().divide(word.sValue());
            this.data = bigIntToBytes32(result.and(MAX_VALUE));
        }
    }

    public void sub(DataWord word) {
        BigInteger result = value().subtract(word.value());
        this.data = bigIntToBytes32(result.and(MAX_VALUE));
    }

    public void exp(DataWord word) {
        BigInteger result = value().modPow(word.value(), TWO_256);
        this.data = bigIntToBytes32(result);
    }

    public void mod(DataWord word) {
        if (word.isZero()) {
            this.and(ZERO);
        } else {
            BigInteger result = value().mod(word.value());
            this.data = bigIntToBytes32(result.and(MAX_VALUE));
        }
    }

    public void sMod(DataWord word) {
        if (word.isZero()) {
            this.and(ZERO);
        } else {
            BigInteger result = sValue().abs().mod(word.sValue().abs());
            result = (sValue().signum() == -1) ? result.negate() : result;

            this.data = bigIntToBytes32(result.and(MAX_VALUE));
        }
    }

    public void addmod(DataWord word1, DataWord word2) {
        if (word2.isZero()) {
            this.data = new byte[32];
        } else {
            BigInteger result = value().add(word1.value()).mod(word2.value());
            this.data = bigIntToBytes32(result.and(MAX_VALUE));
        }
    }

    public void mulmod(DataWord word1, DataWord word2) {
        if (this.isZero() || word1.isZero() || word2.isZero()) {
            this.data = new byte[32];
        } else {
            BigInteger result = value().multiply(word1.value()).mod(word2.value());
            this.data = bigIntToBytes32(result.and(MAX_VALUE));
        }
    }

    @Override
    public String toString() {
        return Hex.encode(data);
    }

    public String toPrefixString() {
        byte[] pref = getNoLeadZeroesData();
        if (pref.length == 0)
            return "";

        if (pref.length < 7)
            return Hex.encode(pref);

        return Hex.encode(pref).substring(0, 6);
    }

    public String shortHex() {
        String hexValue = Hex.encode(getNoLeadZeroesData()).toUpperCase();
        return "0x" + hexValue.replaceFirst("^0+(?!$)", "");
    }

    public DataWord clone() {
        return new DataWord(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        return Arrays.equals(this.data, ((DataWord) o).data);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @Override
    public int compareTo(DataWord o) {
        return org.bouncycastle.util.Arrays.compareUnsigned(this.data, o.data);
    }

    public void signExtend(byte k) {
        if (0 > k || k > 31) {
            throw new IndexOutOfBoundsException();
        }

        byte mask = this.sValue().testBit((k * 8) + 7) ? (byte) 0xff : 0;
        for (int i = 31; i > k; i--) {
            this.data[31 - i] = mask;
        }
    }

    public int bytesOccupied() {
        for (int i = 0; i < 32; i++) {
            if (data[i] != 0) {
                return 32 - i;
            }
        }

        return 0;
    }

    public boolean isHex(String hex) {
        return Hex.encode(data).equals(hex);
    }

    public String asString() {
        return new String(getNoLeadZeroesData());
    }

    private static byte[] bigIntToBytes32(BigInteger num) {
        byte[] result = new byte[32];
        byte[] bigInt = num.toByteArray();
        System.arraycopy(bigInt, 0, result, 32 - bigInt.length, bigInt.length);
        return result;
    }
}
