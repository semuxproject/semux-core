/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import org.bouncycastle.util.Arrays;
import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.net.Peer;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class WorldMessage extends Message {

    private Peer peer;

    private long timestamp;

    private byte[] dataToSign;
    private Signature signature;

    /**
     * Create a WORLD message.
     * 
     * @param peer
     * @param coinbase
     */
    public WorldMessage(Peer peer, EdDSA coinbase) {
        super(MessageCode.WORLD, null);

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
     * Parse a WORLD message from byte array.
     * 
     * @param encoded
     */
    public WorldMessage(byte[] encoded) {
        super(MessageCode.WORLD, null);

        this.encoded = encoded;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.peer = Peer.fromBytes(dec.readBytes());
        this.timestamp = dec.readLong();
        this.dataToSign = Arrays.copyOfRange(encoded, 0, dec.getReadIndex());
        this.signature = Signature.fromBytes(dec.readBytes());
    }

    /**
     * Check if this message is valid at the time when this method is called.
     * 
     * @return
     */
    public boolean isValid() {
        return Math.abs(System.currentTimeMillis() - timestamp) <= Config.NET_HANDSHAKE_EXPIRE
                && EdDSA.verify(dataToSign, signature);
    }

    /**
     * Get the peer information.
     * 
     * @return
     */
    public Peer getPeer() {
        return peer;
    }

    @Override
    public String toString() {
        return "WorldMessage [peer=" + peer + "]";
    }
}