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
import org.semux.util.TimeUtil;

public class PingMessage extends Message {

    private final long timestamp;

    /**
     * Create a PING message.
     * 
     */
    public PingMessage() {
        super(MessageCode.PING, PongMessage.class);

        this.timestamp = TimeUtil.currentTimeMillis();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(timestamp);
        this.body = enc.toBytes();
    }

    /**
     * Parse a PING message from byte array.
     * 
     * @param body
     */
    public PingMessage(byte[] body) {
        super(MessageCode.PING, PongMessage.class);

        SimpleDecoder dec = new SimpleDecoder(body);
        this.timestamp = dec.readLong();

        this.body = body;
    }

    @Override
    public String toString() {
        return "PingMessage [timestamp=" + timestamp + "]";
    }
}
