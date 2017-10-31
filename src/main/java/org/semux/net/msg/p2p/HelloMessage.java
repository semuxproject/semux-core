/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import org.bouncycastle.util.Arrays;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.net.Peer;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class HelloMessage extends Message {

    private Peer peer;

    private long timestamp;

    private byte[] dataToSign;
    private Signature signature;

    /**
     * Create a HELLO message.
     * 
     * @param peer
     * @param coinbase
     */
    public HelloMessage(Peer peer, EdDSA coinbase) {
        super(MessageCode.HELLO, WorldMessage.class);

        this.peer = peer;

        this.timestamp = System.currentTimeMillis();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(peer.toBytes());
        enc.writeLong(timestamp);
        this.dataToSign = enc.toBytes();
        this.signature = coinbase.sign(dataToSign);
        enc.writeBytes(signature.toBytes());

        this.encoded = enc.toBytes();
    }

    /**
     * Parse a HELLO message from byte array.
     * 
     * @param encoded
     */
    public HelloMessage(byte[] encoded) {
        super(MessageCode.HELLO, WorldMessage.class);

        this.encoded = encoded;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.peer = Peer.fromBytes(dec.readBytes());
        this.timestamp = dec.readLong();
        this.dataToSign = Arrays.copyOfRange(encoded, 0, dec.getReadIndex());
        this.signature = Signature.fromBytes(dec.readBytes());
    }

    /**
     * 
     * Validates this HELLO message.
     * 
     * <p>
     * NOTE: only data format and signature is checked here.
     * </p>
     * 
     * @return true if valid, otherwise false
     */
    public boolean validate() {
        return peer != null //
                && timestamp > 0//
                && EdDSA.verify(dataToSign, signature);
    }

    public Peer getPeer() {
        return peer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "HelloMessage [peer=" + peer + "]";
    }
}