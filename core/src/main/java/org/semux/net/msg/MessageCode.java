/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

public enum MessageCode {
    // =======================================
    // [0x00, 0x2f] Reserved for p2p basics
    // =======================================

    /**
     * [0x00] Inform peer of disconnecting.
     */
    DISCONNECT(0x00),

    /**
     * [0x01] First message over connection. No messages should be sent until
     * receiving a response.
     */
    HELLO(0x01),

    /**
     * [0x02] Response to a HELLO message.
     */
    WORLD(0x02),

    /**
     * [0x03] Request an immediate reply from the peer.
     */
    PING(0x03),

    /**
     * [0x04] Response to a PING message.
     */
    PONG(0x04),

    /**
     * [0x05] Request peer to provide a list of known nodes.
     */
    GET_NODES(0x05),

    /**
     * [0x06] Response to a GET_NODES message.
     */
    NODES(0x06),

    /**
     * [0x07] Propagate transaction.
     */
    TRANSACTION(0x07),

    /**
     * [0x08] A message containing a random bytes
     */
    HANDSHAKE_INIT(0x08),

    /**
     * [0x09] The new HELLO message
     */
    HANDSHAKE_HELLO(0x09),

    /**
     * [0x10] The new WORLD message.
     */
    HANDSHAKE_WORLD(0x10),

    // =======================================
    // [0x30, 0x3f] Reserved for sync
    // =======================================

    /**
     * [0x30] Request a block from the peer.
     */
    GET_BLOCK(0x30),

    /**
     * [0x31] Response containing a block.
     */
    BLOCK(0x31),

    /**
     * [0x32] Request a block from the peer.
     */
    GET_BLOCK_HEADER(0x32),

    /**
     * [0x33] Response containing a block.
     */
    BLOCK_HEADER(0x33),

    /**
     * [0x34] Request parts of a block from the peer.
     */
    GET_BLOCK_PARTS(0x34),

    /**
     * [0x35] Response containing the block parts.
     */
    BLOCK_PARTS(0x35),

    // =======================================
    // [0x40, 0x4f] Reserved for BFT
    // =======================================

    /**
     * [0x40] BFT new height message.
     */
    BFT_NEW_HEIGHT(0x40),

    /**
     * [0x41] BFT new height message.
     */
    BFT_NEW_VIEW(0x41),

    /**
     * [0x42] BFT proposal message.
     */
    BFT_PROPOSAL(0x42),

    /**
     * [0x43] BFT vote message.
     */
    BFT_VOTE(0x43);

    private static final MessageCode[] map = new MessageCode[256];

    static {
        for (MessageCode mc : MessageCode.values()) {
            map[mc.code] = mc;
        }
    }

    public static MessageCode of(int code) {
        return map[0xff & code];
    }

    private int code;

    MessageCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public byte toByte() {
        return (byte) code;
    }
}
