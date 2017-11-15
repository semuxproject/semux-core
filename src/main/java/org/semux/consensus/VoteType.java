/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

public enum VoteType {

    /**
     * (0x00) Vote during the VALIDATE period.
     */
    VALIDATE(0x00),

    /**
     * (0x01) Vote during the PRECOMMIT period.
     */
    PRECOMMIT(0x01),

    /**
     * (0x02) Vote during the COMMIT period.
     */
    COMMIT(0x02);

    private static final VoteType[] map = new VoteType[256];
    static {
        for (VoteType tt : VoteType.values()) {
            map[tt.code] = tt;
        }
    }

    public static VoteType of(int code) {
        return map[0xff & code];
    }

    private int code;

    VoteType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public byte toByte() {
        return (byte) code;
    }
}
