/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p.handshake.v2;

import org.semux.Network;
import org.semux.crypto.Key;
import org.semux.net.CapabilitySet;
import org.semux.net.msg.MessageCode;

public class WorldMessage extends HandshakeMessage {

    private static final byte PREFIX = 0x01;

    public WorldMessage(Network network, short networkVersion, String peerId, int port,
            String clientId, CapabilitySet capabilities, long latestBlockNumber,
            Key coinbase) {
        super(PREFIX, MessageCode.WORLD_V2, null, network, networkVersion, peerId, port, clientId,
                capabilities, latestBlockNumber, coinbase);
    }

    public WorldMessage(byte[] encoded) {
        super(PREFIX, MessageCode.WORLD_V2, null, encoded);
    }

    @Override
    public String toString() {
        return "WorldMessage{" +
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