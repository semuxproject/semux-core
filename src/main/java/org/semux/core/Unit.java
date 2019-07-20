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
        BigDecimal $nano = BigDecimal.valueOf(a.getNano());
        return $nano.movePointLeft(exp).setScale(scale, FLOOR);
    }

    public Amount fromDecimal(BigDecimal d) {
        return new Amount(d.movePointRight(exp).setScale(0, FLOOR).longValueExact());
    }
}
