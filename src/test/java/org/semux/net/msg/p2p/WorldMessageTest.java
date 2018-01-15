/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.crypto.Key;
import org.semux.net.Capability;
import org.semux.net.Peer;

public class WorldMessageTest {

    @Test
    public void TestIsValid() {
        Config config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);

        Key key = new Key();
        String peerId = key.toAddressString();
        Peer peer = new Peer("127.0.0.1", 5161, config.networkVersion(), config.getClientId(), peerId, 2,
                Capability.SUPPORTED);

        WorldMessage msg = new WorldMessage(peer, key);
        assertTrue(msg.validate(config));
        assertEquals(key.toAddressString(), msg.getPeer().getPeerId());

        msg = new WorldMessage(msg.getEncoded());
        assertTrue(msg.validate(config));
        assertEquals(key.toAddressString(), msg.getPeer().getPeerId());
    }
}
