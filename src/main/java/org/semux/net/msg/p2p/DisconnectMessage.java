/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.net.msg.ReasonCode;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class DisconnectMessage extends Message {

    private ReasonCode reason;

    /**
     * Create a DISCONNECT message.
     * 
     * @param reason
     */
    public DisconnectMessage(ReasonCode reason) {
        super(MessageCode.DISCONNECT, null);

        this.reason = reason;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeByte(reason.toByte());
        this.encoded = enc.toBytes();
    }

    /**
     * Parse a DISCONNECT message from byte array.
     * 
     * @param encoded
     */
    public DisconnectMessage(byte[] encoded) {
        super(MessageCode.DISCONNECT, null);

        this.encoded = encoded;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.reason = ReasonCode.of(dec.readByte());
    }

    public ReasonCode getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "DisconnectMessage [reason=" + reason + "]";
    }
}