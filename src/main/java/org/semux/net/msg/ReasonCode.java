/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

public enum ReasonCode {

    /**
     * [0x00] Reserved code.
     */
    RESERVED(0x00),

    /**
     * [0x01] Incompatible protocol.
     */
    INCOMPATIBLE_PROTOCOL(0x01),

    /**
     * [0x02] Too many active peers.
     */
    TOO_MANY_PEERS(0x02),

    /**
     * [0x03] Invalid handshake message.
     */
    INVALID_HANDSHAKE(0x03),

    /**
     * [0x04] Duplicated peerId.
     */
    DUPLICATED_PEER_ID(0x04),

    /**
     * [0x05] Message queue full.
     */
    MESSAGE_QUEUE_FULL(0x05),

    /**
     * [0x06] IP address is used for another validator.
     */
    VALIDATOR_IP_LIMITED(0x06),

    /**
     * [0x07]
     */
    HANDSHAKE_EXISTS(0x07),

    /**
     * [0x08] Bad peer, typically due to invalid behavior.
     */
    BAD_PEER(0x08);

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

    ReasonCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public byte toByte() {
        return (byte) code;
    }
}
