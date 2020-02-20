/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p.handshake.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.crypto.Key;

public class WorldMessageTest {

    @Test
    public void testCodec() {
        Config config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);

        Key key = new Key();
        String peerId = key.toAddressString();
        WorldMessage msg = new WorldMessage(config.network(), config.networkVersion(), peerId, "127.0.0.1", 5161,
                config.getClientId(), 2, key);
        assertTrue(msg.validate(config));
        assertEquals(key.toAddressString(), msg.getPeer().getPeerId());

        msg = new WorldMessage(msg.getBody());
        assertTrue(msg.validate(config));
        assertEquals(key.toAddressString(), msg.getPeer().getPeerId());
    }
}
