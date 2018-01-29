/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

public class BigIntegerUtilTest {

    private BigInteger one = BigInteger.valueOf(1);
    private BigInteger two = BigInteger.valueOf(2);
    private BigInteger three = BigInteger.valueOf(3);

    @Test
    public void testIsZero() {
        assertTrue(BigIntegerUtil.isZero(BigInteger.ZERO));
        assertFalse(BigIntegerUtil.isZero(BigInteger.ONE));
    }

    @Test
    public void testIsEqual() {
        assertTrue(BigIntegerUtil.isEqual(two, two));
        assertFalse(BigIntegerUtil.isEqual(two, three));
    }

    @Test(expected = NullPointerException.class)
    public void testIsEqual2() {
        assertFalse(BigIntegerUtil.isEqual(two, null));
    }

    @Test
    public void testIsNotEqual() {
        assertFalse(BigIntegerUtil.isNotEqual(two, two));
        assertTrue(BigIntegerUtil.isNotEqual(two, three));
    }

    @Test(expected = NullPointerException.class)
    public void testIsNotEqual2() {
        assertTrue(BigIntegerUtil.isNotEqual(two, null));
    }

    @Test
    public void testIsLessThan() {
        assertTrue(BigIntegerUtil.isLessThan(two, three));
        assertFalse(BigIntegerUtil.isLessThan(two, two));
        assertFalse(BigIntegerUtil.isLessThan(two, one));
    }

    @Test
    public void testIsGreaterThan() {
        assertTrue(BigIntegerUtil.isGreaterThan(two, one));
        assertFalse(BigIntegerUtil.isGreaterThan(two, two));
        assertFalse(BigIntegerUtil.isGreaterThan(two, three));
    }

    @Test
    public void testIsPositive() {
        assertTrue(BigIntegerUtil.isPositive(one));
        assertFalse(BigIntegerUtil.isPositive(one.negate()));
    }

    @Test
    public void testIsNegative() {
        assertFalse(BigIntegerUtil.isNegative(one));
        assertTrue(BigIntegerUtil.isNegative(one.negate()));
    }

    @Test
    public void testMax() {
        assertEquals(two, BigIntegerUtil.max(two, one));
        assertEquals(two, BigIntegerUtil.max(one, two));
    }

    @Test
    public void testMin() {
        assertEquals(one, BigIntegerUtil.min(two, one));
        assertEquals(one, BigIntegerUtil.min(one, two));
    }

    @Test
    public void testSum() {
        assertEquals(three, BigIntegerUtil.sum(one, two));
    }
}
