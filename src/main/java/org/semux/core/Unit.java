/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Unit {
    public static final long NANO_SEM = 1L;

    public static final long MICRO_SEM = 1_000L;

    public static final long MILLI_SEM = 1_000_000L;

    public static final long SEM = 1_000_000_000L;

    public static final long KILO_SEM = 1_000_000_000_000L;

    public static final long MEGA_SEM = 1_000_000_000_000_000L;

    public static final Map<String, Long> SUPPORTED;

    static {
        Map<String, Long> aMap = new HashMap<>();
        aMap.put("μSEM", MICRO_SEM);
        aMap.put("mSEM", MILLI_SEM);
        aMap.put("SEM", SEM);
        SUPPORTED = Collections.unmodifiableMap(aMap);
    }

    /**
     * Converts an unit string into its decimal value.
     *
     * @param unit
     * @return
     */
    public static long valueOf(String unit) {
        if (SUPPORTED.containsKey(unit)) {
            return SUPPORTED.get(unit);
        } else {
            throw new IllegalArgumentException(String.format("%s is an unsupported unit.", unit));
        }
    }

    private Unit() {
    }
}
