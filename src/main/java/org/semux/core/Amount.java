/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.math.BigInteger;

public final class Amount {

    private final long nano;
    public static final Amount ZERO = new Amount(0);

    public Amount(long nano) {
        this.nano = nano;
    }

    public long getNano() {
        return nano;
    }

    public BigInteger getBigInteger() {
        return BigInteger.valueOf(nano);
    }

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
        return Unit.SEM.toDecimal(this, 9).stripTrailingZeros().toPlainString() + " SEM";
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
