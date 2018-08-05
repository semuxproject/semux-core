/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p.handshake.v2;

import org.semux.crypto.Hex;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class InitMessage extends Message {

    public static final int SECRET_LENGTH = 32;

    private byte[] secret;
    private long timestamp;

    public InitMessage(byte[] secret, long timestamp) {
        super(MessageCode.HANDSHAKE_INIT, null);

        this.secret = secret;
        this.timestamp = timestamp;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(secret);
        enc.writeLong(timestamp);

        this.encoded = enc.toBytes();
    }

    public InitMessage(byte[] encoded) {
        super(MessageCode.HANDSHAKE_INIT, null);

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.secret = dec.readBytes();
        this.timestamp = dec.readLong();

        this.encoded = encoded;
    }

    public boolean validate() {
        return secret != null && secret.length == SECRET_LENGTH
                && timestamp > 0;
    }

    public byte[] getSecret() {
        return secret;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "InitMessage{" +
                "secret=" + Hex.encode(secret) +
                ", timestamp=" + timestamp +
                '}';
    }
}
