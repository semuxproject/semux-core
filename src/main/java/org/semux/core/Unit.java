/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static java.util.Arrays.stream;

import java.math.BigInteger;

public enum Unit {
    NANO_SEM(0, "nSEM"),

    MICRO_SEM(3, "μSEM"),

    MILLI_SEM(6, "mSEM"),

    SEM(9, "SEM"),

    KILO_SEM(12, "kSEM"),

    MEGA_SEM(15, "MSEM");

    public final int exp;
    public final long factor;
    public final String symbol;

    Unit(int exp, String symbol) {
        this.exp = exp;
        this.factor = BigInteger.TEN.pow(exp).longValueExact();
        this.symbol = symbol;
    }

    /**
     * Decode the unit from symbol.
     *
     * @param symbol
     *            the symbol text
     * @return a Unit object if valid; otherwise false
     */
    public static Unit of(String symbol) {
        return stream(values()).filter(v -> v.symbol.equals(symbol)).findAny().orElse(null);
    }
}
