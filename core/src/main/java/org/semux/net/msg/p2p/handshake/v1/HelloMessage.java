/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p.handshake.v1;

import org.semux.Network;
import org.semux.config.Config;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.Key.Signature;
import org.semux.net.Peer;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.semux.util.TimeUtil;

public class HelloMessage extends Message {

    private final Peer peer;
    private final long timestamp;
    private final Signature signature;

    /**
     * Create a HELLO message.
     */
    public HelloMessage(Network network, short networkVersion, String peerId, String ip, int port, String clientId,
            long latestBlockNumber, Key coinbase) {
        super(MessageCode.HELLO, WorldMessage.class);

        this.peer = new Peer(network, networkVersion, peerId, ip, port, clientId,
                PeerCodec.mandatoryCapabilities(network), latestBlockNumber);
        this.timestamp = TimeUtil.currentTimeMillis();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(PeerCodec.toBytes(peer));
        enc.writeLong(timestamp);
        this.signature = coinbase.sign(enc.toBytes());
        enc.writeBytes(signature.toBytes());

        this.body = enc.toBytes();
    }

    /**
     * Parse a HELLO message from byte array.
     *
     * @param body
     */
    public HelloMessage(byte[] body) {
        super(MessageCode.HELLO, WorldMessage.class);

        SimpleDecoder dec = new SimpleDecoder(body);
        this.peer = PeerCodec.fromBytes(dec.readBytes());
        this.timestamp = dec.readLong();
        this.signature = Signature.fromBytes(dec.readBytes());

        this.body = body;
    }

    /**
     * Validates this HELLO message.
     *
     * <p>
     * NOTE: only data format and signature is checked here.
     * </p>
     *
     * @param config
     * @return true if success, otherwise false
     */
    public boolean validate(Config config) {
        if (peer != null && PeerCodec.validate(peer)
                && Math.abs(TimeUtil.currentTimeMillis() - timestamp) <= config.netHandshakeExpiry()
                && signature != null
                && peer.getPeerId().equals(Hex.encode(signature.getAddress()))) {

            SimpleEncoder enc = new SimpleEncoder();
            enc.writeBytes(PeerCodec.toBytes(peer));
            enc.writeLong(timestamp);

            return Key.verify(enc.toBytes(), signature);
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
        return "HelloMessage [peer=" + peer + "]";
    }
}