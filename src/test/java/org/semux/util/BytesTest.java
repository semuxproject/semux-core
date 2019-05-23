/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Test;
import org.semux.crypto.Hex;

public class BytesTest {

    @Test
    public void testRandom() {
        int n = 20;
        byte[] bytes1 = Bytes.random(n);
        byte[] bytes2 = Bytes.random(n);

        assertEquals(n, bytes1.length);
        assertEquals(n, bytes2.length);
        assertFalse(Arrays.equals(bytes1, bytes2));
    }

    @Test
    public void testMerge() {
        byte[] bytes1 = Bytes.of("Hello");
        byte[] bytes2 = Bytes.of("World");
        assertEquals("HelloWorld", Bytes.toString(Bytes.merge(bytes1, bytes2)));
    }

    @Test
    public void testString() {
        byte[] bytes1 = Bytes.of("test");
        byte[] bytes2 = Bytes.of(Bytes.toString(bytes1));
        assertArrayEquals(bytes1, bytes2);
    }

    @Test
    public void testShort() {
        short s1 = Short.MIN_VALUE;
        short s2 = 0;
        short s3 = Short.MAX_VALUE;

        assertEquals(s1, Bytes.toShort(Bytes.of(s1)));
        assertEquals(s2, Bytes.toShort(Bytes.of(s2)));
        assertEquals(s3, Bytes.toShort(Bytes.of(s3)));
    }

    @Test
    public void testInt() {
        int i1 = Integer.MIN_VALUE;
        int i2 = 0;
        int i3 = Integer.MAX_VALUE;

        assertEquals(i1, Bytes.toInt(Bytes.of(i1)));
        assertEquals(i2, Bytes.toInt(Bytes.of(i2)));
        assertEquals(i3, Bytes.toInt(Bytes.of(i3)));
    }

    @Test
    public void testLong() {
        long l1 = Long.MIN_VALUE;
        long l2 = 0;
        long l3 = Long.MAX_VALUE;

        assertEquals(l1, Bytes.toLong(Bytes.of(l1)));
        assertEquals(l2, Bytes.toLong(Bytes.of(l2)));
        assertEquals(l3, Bytes.toLong(Bytes.of(l3)));
    }

    @Test
    public void testBigInteger() {
        assertBigInteger(Hex.decode("A08601"), BigInteger.valueOf(100000));
        assertBigInteger(Hex.decode("16EB"), BigInteger.valueOf(-5354));
        assertBigInteger(Hex.decode("0080"), BigInteger.valueOf(-32768));
        assertBigInteger(Hex.decode("01F81D7AF1971CEDD9BBA5EFCEE100"), BigInteger.valueOf(127).pow(16));
    }

    private void assertBigInteger(byte[] bytes, BigInteger value) {
        assertArrayEquals(bytes, Bytes.of(value));
        assertEquals(value, Bytes.toBigInteger(bytes));
    }
}
