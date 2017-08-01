/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;

public class PeerClientTest {

    @Test
    public void testServer() throws InterruptedException {
        EdDSA key1 = new EdDSA();
        PeerClient remoteClient = new PeerClient("127.0.0.1", 5161, key1);
        InetSocketAddress remoteAddress = new InetSocketAddress(remoteClient.getIp(), remoteClient.getPort());

        PeerServerMock ps = new PeerServerMock();
        ps.start(remoteClient, true);
        assertTrue(ps.getServer().isListening());

        try {
            EdDSA key2 = new EdDSA();
            PeerClient client = new PeerClient("127.0.0.1", 5162, key2);

            Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
            ChannelManager channelMgr = new ChannelManager();
            NodeManager nodeMgr = new NodeManager(chain, channelMgr, client);

            SemuxChannelInitializer ci = new SemuxChannelInitializer(chain, channelMgr, nodeMgr, client, remoteAddress);
            client.connectAsync(remoteAddress, ci).sync();

            // waiting for the HELLO message to be sent
            Thread.sleep(1000);
            assertEquals(1, channelMgr.getActivePeers().size());

            client.close();
            assertEquals(0, channelMgr.getActivePeers().size());
        } finally {
            ps.stop();
            assertFalse(ps.getServer().isListening());
        }
    }
}
