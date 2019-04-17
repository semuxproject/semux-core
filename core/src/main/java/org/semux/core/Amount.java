/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static java.math.RoundingMode.FLOOR;
import static java.util.Arrays.stream;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class Amount {

    public enum Unit {
        NANO_SEM(0, "nSEM"),

        MICRO_SEM(3, "μSEM"),

        MILLI_SEM(6, "mSEM"),

        SEM(9, "SEM"),

        KILO_SEM(12, "kSEM"),

        MEGA_SEM(15, "MSEM");

        private final int exp;
        private final long factor;
        public final String symbol;

        Unit(int exp, String symbol) {
            this.exp = exp;
            this.factor = BigInteger.TEN.pow(exp).longValueExact();
            this.symbol = symbol;
        }

        public static Unit ofSymbol(String s) {
            return stream(values()).filter(i -> s.equals(i.symbol)).findAny().get();
        }

        public Amount of(long a) {
            return new Amount(Math.multiplyExact(a, factor));
        }

        public Amount ofGas(long gas, long gasPrice) {
            return new Amount(Math.multiplyExact(gas, gasPrice));
        }

        public BigDecimal toDecimal(Amount a, int scale) {
            BigDecimal $nano = BigDecimal.valueOf(a.nano);
            return $nano.movePointLeft(exp).setScale(scale, FLOOR);
        }

        public Amount fromDecimal(BigDecimal d) {
            return new Amount(d.movePointRight(exp).setScale(0, FLOOR).longValueExact());
        }
    }

    private final long nano;
    public static final Amount ZERO = new Amount(0);

    private Amount(long nano) {
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

}
