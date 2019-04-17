/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigIntegerUtilTest {

    private static Logger logger = LoggerFactory.getLogger(BigIntegerUtil.class);

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

    /**
     * Test if the random function is uniformly distributed. Credits to:
     * https://github.com/dwdyer/uncommons-maths/blob/462c043ffbc8df4bd45c490e447ea1ba636b1f15/core/src/java/test/org/uncommons/maths/random/DiscreteUniformGeneratorTest.java
     */
    @Test
    public void testRandomUniformlyDistributed() {
        final int N = 10000, MAX = 100;
        BigInteger[] data = new BigInteger[N];
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < N; i++) {
            data[i] = BigIntegerUtil.random(BigInteger.valueOf(i)).mod(BigInteger.valueOf(MAX));
            sum = sum.add(data[i]);
        }

        BigDecimal mean = new BigDecimal(sum).divide(BigDecimal.valueOf(N), MathContext.DECIMAL128);
        BigDecimal squaredDiffs = BigDecimal.ZERO;
        for (int i = 0; i < N; i++) {
            BigDecimal diff = mean.subtract(new BigDecimal(data[i]));
            squaredDiffs = squaredDiffs.add(diff.pow(2));
        }
        BigDecimal variance = squaredDiffs.divide(BigDecimal.valueOf(N), MathContext.DECIMAL128);
        BigDecimal deviation = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        BigDecimal expectedDeviation = BigDecimal.valueOf(MAX / Math.sqrt(12));
        BigDecimal expectedMean = BigDecimal.valueOf(MAX).divide(BigDecimal.valueOf(2), MathContext.DECIMAL128);
        logger.info("Mean = {}, Expected Mean = {}", mean, expectedMean);
        logger.info("Deviation = {}, Expected Deviation = {}", deviation, expectedDeviation);

        assertThat("deviation",
                deviation.subtract(expectedDeviation).abs(),
                lessThanOrEqualTo(BigDecimal.valueOf(0.02)));
        assertThat("mean",
                mean.subtract(expectedMean).abs(),
                lessThanOrEqualTo(BigDecimal.valueOf(1)));
    }
}
