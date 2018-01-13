/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.crypto.Key;

public class PeerTest {

    @Test
    public void testPeer() {
        String ip = "127.0.0.1";
        int port = 1234;
        short p2pVersion = 2;
        String clientId = "client";
        String peerId = new Key().toAddressString();
        long latestBlockNumber = 1;

        Peer peer = new Peer(ip, port, p2pVersion, clientId, peerId, latestBlockNumber);
        peer = Peer.fromBytes(peer.toBytes());

        assertEquals(ip, peer.getIp());
        assertEquals(port, peer.getPort());
        assertEquals(p2pVersion, peer.getNetworkVersion());
        assertEquals(clientId, peer.getClientId());
        assertEquals(peerId, peer.getPeerId());
        assertEquals(latestBlockNumber, peer.getLatestBlockNumber());
    }
}
