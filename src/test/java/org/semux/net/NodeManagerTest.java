/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.config.Constants;
import org.semux.rules.KernelRule;

public class NodeManagerTest {

    private PeerServerMock server1;
    private PeerServerMock server2;

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Rule
    public KernelRule kernelRule2 = new KernelRule(51620, 51720);

    @Before
    public void setUp() {
        server1 = new PeerServerMock(kernelRule1.getKernel());
        server1.start();
    }

    @After
    public void tearDown() {
        if (server1 != null) {
            server1.stop();
        }
        if (server2 != null) {
            server2.stop();
        }
    }

    @Test
    public void testGetSeedNodes() {
        // Seed nodes for main net
        Set<NodeManager.Node> peers = NodeManager.getSeedNodes(Constants.MAIN_NET_ID);
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
        server2 = new PeerServerMock(kernelRule2.getKernel());
        server2.start();

        KernelMock kernel2 = kernelRule2.getKernel();
        NodeManager nodeMgr = kernel2.getNodeManager();
        nodeMgr.addNode(new NodeManager.Node("127.0.0.1", server1.getKernel().getConfig().p2pListenPort()));
        nodeMgr.doConnect();

        Thread.sleep(500);
        assertFalse(kernel2.getChannelManager().getActivePeers().isEmpty());
    }
}
