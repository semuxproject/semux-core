/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static java.math.RoundingMode.FLOOR;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents an amount of {@link Unit#NANO_SEM}, the base unit of computation.
 */
public final class Amount implements Comparable<Amount> {

    public static final Amount ZERO = new Amount(0);
    public static final Amount ONE = new Amount(1);
    public static final Amount TEN = new Amount(10);

    private final long nano;

    private Amount(long nano) {
        this.nano = nano;
    }

    /**
     * Converts n nSEM to an amount.
     *
     * @param n
     *            the number of nSEM
     * @return
     */
    public static Amount of(long n) {
        return new Amount(n);
    }

    /**
     * Converts n nSEM to an amount.
     *
     * @param n
     *            the number of nSEM
     * @return
     */
    public static Amount of(String n) {
        return new Amount(Long.parseLong(n));
    }

    /**
     * Converts n units to an amount.
     *
     * @param n
     *            the number of unit
     * @param unit
     *            the unit
     * @return an Amount
     * @throws ArithmeticException
     *             if the value overflows
     */
    public static Amount of(long n, Unit unit) throws ArithmeticException {
        return new Amount(Math.multiplyExact(n, unit.factor));
    }

    /**
     * Converts a BigDecimal of units to an amount.
     *
     * @param d
     *            the big decimal
     * @param unit
     *            the unit to use when converting
     * @return an Amount
     */
    public static Amount of(BigDecimal d, Unit unit) {
        return new Amount(d.movePointRight(unit.exp).setScale(0, FLOOR).longValueExact());
    }

    /**
     * Converts this amount to a BigDecimal.
     *
     * @param scale
     *            the scale of digits
     * @param unit
     *            the unit to use when converting.
     * @return A BigDecimal
     */
    public BigDecimal toDecimal(int scale, Unit unit) {
        BigDecimal nano = BigDecimal.valueOf(this.nano);
        return nano.movePointLeft(unit.exp).setScale(scale, FLOOR);
    }

    /**
     * Converts this amount to a long integer.
     *
     * @return a long integer.
     */
    public long toLong() {
        return nano;
    }

    /**
     * Converts this amount to a BigInteger.
     *
     * @return a BigInteger
     */
    public BigInteger toBigInteger() {
        return BigInteger.valueOf(nano);
    }

    @Override
    public int compareTo(Amount other) {
        return this.lessThan(other) ? -1 : (this.greaterThan(other) ? 1 : 0);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(nano);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Amount && ((Amount) other).nano == nano;
    }

    @Override
    public String toString() {
        return String.valueOf(nano);
    }

    public boolean greaterThan(Amount other) {
        return nano > other.nano;
    }

    public boolean greaterThanOrEqual(Amount other) {
        return nano >= other.nano;
    }

    public boolean isPositive() {
        return greaterThan(ZERO);
    }

    public boolean isNotNegative() {
        return greaterThanOrEqual(ZERO);
    }

    public boolean lessThan(Amount other) {
        return nano < other.nano;
    }

    public boolean lessThanOrEqual(Amount other) {
        return nano <= other.nano;
    }

    public boolean isNegative() {
        return lessThan(ZERO);
    }

    public boolean isNotPositive() {
        return lessThanOrEqual(ZERO);
    }

    public Amount negate() throws ArithmeticException {
        return new Amount(Math.negateExact(this.nano));
    }

    public Amount add(Amount a) throws ArithmeticException {
        return new Amount(Math.addExact(this.nano, a.nano));
    }

    public Amount subtract(Amount a) throws ArithmeticException {
        return new Amount(Math.subtractExact(this.nano, a.nano));
    }

    public Amount multiply(long a) throws ArithmeticException {
        return new Amount(Math.multiplyExact(this.nano, a));
    }

    public static Amount sum(Amount a, Amount b) throws ArithmeticException {
        return new Amount(Math.addExact(a.nano, b.nano));
    }
}
