/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hex;
import org.semux.net.Peer;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class WorldMessage extends Message {

    private Peer peer;
    private long timestamp;
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
        this.signature = coinbase.sign(enc.toBytes());
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

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.peer = Peer.fromBytes(dec.readBytes());
        this.timestamp = dec.readLong();
        this.signature = Signature.fromBytes(dec.readBytes());

        this.encoded = encoded;
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
        if (peer != null && peer.validate() //
                && Math.abs(System.currentTimeMillis() - timestamp) <= Config.NET_HANDSHAKE_EXPIRE //
                && signature != null //
                && peer.getPeerId().equals(Hex.encode(signature.getAddress()))) {

            SimpleEncoder enc = new SimpleEncoder();
            enc.writeBytes(peer.toBytes());
            enc.writeLong(timestamp);

            return EdDSA.verify(enc.toBytes(), signature);
        } else {
            return false;
        }
    }

    public Peer getPeer() {
        return peer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "WorldMessage [peer=" + peer + "]";
    }
}