/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.math.BigInteger;

public class BigIntegerUtil {

    /**
     * Returns if the big integer is zero.
     * 
     * @param v
     * @return
     */
    public static boolean isZero(BigInteger v) {
        return v.compareTo(BigInteger.ZERO) == 0;
    }

    /**
     * Returns if the two big integer are equal.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static boolean isEqual(BigInteger v1, BigInteger v2) {
        return v1.compareTo(v2) == 0;
    }

    /**
     * Returns if the two big integer are not equal.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static boolean isNotEqual(BigInteger v1, BigInteger v2) {
        return !isEqual(v1, v2);
    }

    /**
     * Returns if the first big integer is less than the second one.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static boolean isLessThan(BigInteger v1, BigInteger v2) {
        return v1.compareTo(v2) < 0;
    }

    /**
     * Returns if the first big integer is greater than the second one.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static boolean isGreaterThan(BigInteger v1, BigInteger v2) {
        return v1.compareTo(v2) > 0;
    }

    /**
     * Returns if the big integer is positive.
     * 
     * @param v
     * @return
     */
    public static boolean isPositive(BigInteger v) {
        return v.signum() > 0;
    }

    /**
     * Returns if the big integer is negative.
     * 
     * @param v
     * @return
     */
    public static boolean isNegative(BigInteger v) {
        return v.signum() < 0;
    }

    /**
     * Returns the sum of the big integers.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static BigInteger sum(BigInteger v1, BigInteger v2) {
        return v1.add(v2);
    }

    /**
     * Returns the larger one of two big integers.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static BigInteger max(BigInteger v1, BigInteger v2) {
        return v1.compareTo(v2) < 0 ? v2 : v1;
    }

    /**
     * Returns the smaller one of two big integers.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static BigInteger min(BigInteger v1, BigInteger v2) {
        return v1.compareTo(v2) < 0 ? v1 : v2;
    }

    /**
     * Fast pseudo random generator based on LCG Algorithm. Credits to:
     * https://software.intel.com/en-us/articles/fast-random-number-generator-on-the-intel-pentiumr-4-processor
     *
     * @param seed
     * @return
     */
    public static BigInteger random(BigInteger seed) {
        // scramble the seed, credits to:
        // http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/tip/src/share/classes/java/util/Random.java#l145
        seed = seed.xor(BigInteger.valueOf(0x5DEECE66DL)).and(BigInteger.valueOf((1L << 48) - 1));

        final BigInteger a = BigInteger.valueOf(214013L);
        final BigInteger c = BigInteger.valueOf(2531011L);
        final BigInteger m = BigInteger.valueOf(0x7FFFL);
        return a.multiply(seed).add(c).shiftRight(16).and(m);
    }

    private BigIntegerUtil() {
    }
}
