/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class PingMessage extends Message {

    private long timestamp;

    /**
     * Create a PING message.
     * 
     */
    public PingMessage() {
        super(MessageCode.PING, PongMessage.class);

        this.timestamp = System.currentTimeMillis();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(timestamp);
        this.encoded = enc.toBytes();
    }

    /**
     * Parse a PING message from byte array.
     * 
     * @param encoded
     */
    public PingMessage(byte[] encoded) {
        super(MessageCode.PING, PongMessage.class);

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.timestamp = dec.readLong();

        this.encoded = encoded;
    }

    @Override
    public String toString() {
        return "PingMessage [timestamp=" + timestamp + "]";
    }
}
