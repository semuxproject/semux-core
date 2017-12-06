/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.config.Constants;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.crypto.EdDSA;
import org.semux.rules.TemporaryDBRule;

public class NodeManagerTest {

    private static final String P2P_IP = "127.0.0.1";
    private static final int P2P_PORT = 15161;

    @Rule
    public TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    @Test
    public void testGetSeedNodes() {
        // Seed nodes for main net
        Set<InetSocketAddress> peers = NodeManager.getSeedNodes(Constants.MAIN_NET_ID);
        assertFalse(peers.isEmpty());

        // Seed nodes for test net
        peers = NodeManager.getSeedNodes(Constants.TEST_NET_ID);
        assertFalse(peers.isEmpty());

        // Seed nodes for dev net
        peers = NodeManager.getSeedNodes(Constants.DEV_NET_ID);
        assertTrue(peers.isEmpty());
    }

    @Test
    public void testConnect() throws InterruptedException {
        // start server
        PeerServerMock ps = new PeerServerMock(temporaryDBFactory);
        ps.start(P2P_IP, P2P_PORT);

        try {
            EdDSA key = new EdDSA();
            PeerClient client = new PeerClient(P2P_IP, P2P_PORT + 1, key);

            KernelMock kernel = new KernelMock();
            kernel.setBlockchain(new BlockchainImpl(kernel.getConfig(), temporaryDBFactory));
            kernel.setClient(client);
            kernel.setChannelManager(new ChannelManager(kernel));
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setNodeManager(new NodeManager(kernel));
            kernel.setConsensus(new SemuxBFT(kernel));
            kernel.setSyncManager(new SemuxSync(kernel));

            NodeManager nodeMgr = kernel.getNodeManager();
            nodeMgr.addNode(new InetSocketAddress(P2P_IP, P2P_PORT));
            nodeMgr.doConnect();

            Thread.sleep(500);
            assertFalse(kernel.getChannelManager().getActivePeers().isEmpty());
        } finally {
            ps.stop();
        }
    }
}
