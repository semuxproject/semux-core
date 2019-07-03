/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p.handshake.v2;

import org.semux.Network;
import org.semux.crypto.Key;
import org.semux.net.CapabilityList;
import org.semux.net.msg.MessageCode;

public class HelloMessage extends HandshakeMessage {

    public HelloMessage(Network network, short networkVersion, String peerId, int port,
            String clientId, CapabilityList capabilities, long latestBlockNumber,
            byte[] secret, Key coinbase) {
        super(MessageCode.HANDSHAKE_HELLO, WorldMessage.class, network, networkVersion, peerId, port, clientId,
                capabilities, latestBlockNumber, secret, coinbase);
    }

    public HelloMessage(byte[] encoded) {
        super(MessageCode.HANDSHAKE_HELLO, WorldMessage.class, encoded);
    }

    @Override
    public String toString() {
        return "HelloMessage{" +
                "network=" + network +
                ", networkVersion=" + networkVersion +
                ", peerId='" + peerId + '\'' +
                ", port=" + port +
                ", clientId='" + clientId + '\'' +
                ", capabilities=" + capabilities +
                ", latestBlockNumber=" + latestBlockNumber +
                '}';
    }
}