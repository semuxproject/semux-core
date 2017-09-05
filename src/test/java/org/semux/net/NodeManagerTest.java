/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.Config;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;

public class NodeManagerTest {

    private static byte network;

    @BeforeClass
    public static void setup() {
        network = Config.NETWORK_ID;
        Config.NETWORK_ID = 2;
    }

    @Test
    public void testGetSeedNodes() {
        // Seed nodes for main net
        Set<InetSocketAddress> peers = NodeManager.getSeedNodes((short) 0);
        assertFalse(peers.isEmpty());

        // Seed nodes for test net
        peers = NodeManager.getSeedNodes((short) 1);
        assertFalse(peers.isEmpty());

        // Seed nodes for dev net
        peers = NodeManager.getSeedNodes((short) 2);
        assertTrue(peers.isEmpty());
    }

    @Test
    public void testGetPersistedNodes() {
        Set<InetSocketAddress> nodes = new HashSet<>();
        NodeManager.setPersistedNodes(Config.NETWORK_ID, nodes);
        assertTrue(NodeManager.getPersistedNodes(Config.NETWORK_ID).isEmpty());

        nodes.add(new InetSocketAddress("127.0.0.1", 1234));
        NodeManager.setPersistedNodes(Config.NETWORK_ID, nodes);
        assertFalse(NodeManager.getPersistedNodes(Config.NETWORK_ID).isEmpty());
    }

    @Test
    public void testConnAndSync() throws InterruptedException {
        EdDSA key = new EdDSA();
        PeerClient remoteClient = new PeerClient("127.0.0.1", 5161, key);

        // start server
        PeerServerMock ps = new PeerServerMock();
        ps.start(remoteClient, true);

        try {
            PeerClient client = new PeerClient("127.0.0.1", 5162, new EdDSA());

            Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
            ChannelManager channelMgr = new ChannelManager();
            PendingManager pendingMgr = new PendingManager(chain, channelMgr);
            NodeManager nodeMgr = new NodeManager(chain, channelMgr, pendingMgr, client);
            nodeMgr.start();

            nodeMgr.addNode(new InetSocketAddress(remoteClient.getIp(), remoteClient.getPort()));

            Thread.sleep(1000);
            assertFalse(channelMgr.getActivePeers().isEmpty());
        } finally {
            ps.stop();
        }
    }

    @AfterClass
    public static void teardown() {
        Config.NETWORK_ID = network;
    }
}
