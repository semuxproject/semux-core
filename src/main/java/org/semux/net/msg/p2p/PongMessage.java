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

public class PongMessage extends Message {

    private long timestamp;

    /**
     * Create a PONG message.
     */
    public PongMessage() {
        super(MessageCode.PONG, null);

        this.timestamp = System.currentTimeMillis();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(timestamp);
        this.encoded = enc.toBytes();
    }

    /**
     * Parse a PONG message from byte array.
     * 
     * @param encoded
     */
    public PongMessage(byte[] encoded) {
        super(MessageCode.PONG, null);

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.timestamp = dec.readLong();

        this.encoded = encoded;
    }

    @Override
    public String toString() {
        return "PongMessage [timestamp=" + timestamp + "]";
    }
}
