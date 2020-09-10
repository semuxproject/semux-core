/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

public enum ReasonCode {

    /**
     * [0x00] Bad network.
     */
    BAD_NETWORK(0x00),

    /**
     * [0x01] Incompatible protocol.
     */
    BAD_NETWORK_VERSION(0x01),

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
     * [0x05] The message queue is full.
     */
    MESSAGE_QUEUE_FULL(0x05),

    /**
     * [0x06] Another validator peer tries to connect using the same IP.
     */
    VALIDATOR_IP_LIMITED(0x06),

    /**
     * [0x07] The peer tries to re-handshake.
     */
    HANDSHAKE_EXISTS(0x07),

    /**
     * [0x08] The manifests malicious behavior.
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
