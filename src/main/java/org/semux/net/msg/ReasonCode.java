/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

public enum ReasonCode {

    /**
     * [0x00] Requested by the other peer.
     */
    REQUESTED(0x00),

    /**
     * [0x01] Incompatible protocol.
     */
    BAD_PROTOCOL(0x01),

    /**
     * [0x02] Too many active peers.
     */
    TOO_MANY_PEERS(0x02),

    /**
     * [0x03] Bad peerId.
     */
    BAD_PEER_ID(0x03),

    /**
     * [0x04] Duplicate peerId.
     */
    DUPLICATE_PEER_ID(0x04),

    /**
     * [0x05] Slow peer.
     */
    SLOW_PEER(0x05),

    /**
     * [0x06] Consensus error.
     */
    CONSENSUS_ERROR(0x06);

    private int code;

    private static final ReasonCode[] intToCode = new ReasonCode[256];

    static {
        for (ReasonCode mc : ReasonCode.values()) {
            intToCode[mc.code] = mc;
        }
    }

    public static ReasonCode of(int code) {
        return intToCode[0xff & code];
    }

    private ReasonCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public byte toByte() {
        return (byte) code;
    }
}
