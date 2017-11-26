/**
 * Copyright (c) 2017 The Semux Developers
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
}
