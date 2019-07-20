/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static java.math.RoundingMode.FLOOR;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class Amount implements Comparable<Amount> {

    public static final Amount ZERO = new Amount(0);
    public static final Amount ONE_SEM = Amount.of(1, Unit.SEM);
    public static final Amount ONE_NANO_SEM = Amount.of(1, Unit.NANO_SEM);

    private final long nano;

    private Amount(long nano) {
        this.nano = nano;
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
    public long toNanoLong() {
        return nano;
    }

    /**
     * Converts this amount to a BigInteger.
     *
     * @return a BigInteger
     */
    public BigInteger toNanoBigInteger() {
        return BigInteger.valueOf(nano);
    }

    @Override
    public int compareTo(Amount other) {
        return this.lt(other) ? -1 : (this.gt(other) ? 1 : 0);
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
        return this.toDecimal(9, Unit.SEM).stripTrailingZeros().toPlainString() + " SEM";
    }

    public boolean gt(Amount other) {
        return nano > other.nano;
    }

    public boolean gte(Amount other) {
        return nano >= other.nano;
    }

    public boolean gt0() {
        return gt(ZERO);
    }

    public boolean gte0() {
        return gte(ZERO);
    }

    public boolean lt(Amount other) {
        return nano < other.nano;
    }

    public boolean lte(Amount other) {
        return nano <= other.nano;
    }

    public boolean lt0() {
        return lt(ZERO);
    }

    public boolean lte0() {
        return lte(ZERO);
    }

    public static Amount neg(Amount a) {
        return new Amount(Math.negateExact(a.nano));
    }

    public static Amount sum(Amount a1, Amount a2) {
        return new Amount(Math.addExact(a1.nano, a2.nano));
    }

    public static Amount sub(Amount a1, Amount a2) {
        return new Amount(Math.subtractExact(a1.nano, a2.nano));
    }

    public static Amount mul(Amount a1, long a2) {
        return new Amount(Math.multiplyExact(a1.nano, a2));
    }

}
