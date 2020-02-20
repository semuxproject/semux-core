/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p.handshake.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.crypto.Key;
import org.semux.net.CapabilityTreeSet;
import org.semux.net.Peer;
import org.semux.util.Bytes;

public class HelloMessageTest {

    @Test
    public void testCodec() {
        Config config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);

        Key key = new Key();
        String peerId = key.toAddressString();
        HelloMessage msg = new HelloMessage(config.network(), config.networkVersion(), peerId, 5161,
                config.getClientId(), config.getClientCapabilities().toArray(), 2,
                Bytes.random(InitMessage.SECRET_LENGTH), key);
        assertTrue(msg.validate(config));

        msg = new HelloMessage(msg.getBody());
        assertTrue(msg.validate(config));

        String ip = "127.0.0.2";
        Peer peer = msg.getPeer(ip);
        assertEquals(config.network(), peer.getNetwork());
        assertEquals(config.networkVersion(), peer.getNetworkVersion());
        assertEquals(key.toAddressString(), peer.getPeerId());
        assertEquals(ip, peer.getIp());
        assertEquals(config.p2pListenPort(), peer.getPort());
        assertEquals(config.getClientId(), peer.getClientId());
        assertEquals(config.getClientCapabilities(), CapabilityTreeSet.of(peer.getCapabilities()));
        assertEquals(2, peer.getLatestBlockNumber());
    }
}
