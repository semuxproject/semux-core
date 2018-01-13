/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.awaitility.Awaitility.await;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.net.NodeManager.Node;
import org.semux.rules.KernelRule;

public class PeerClientTest {

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
    public void testConnect() throws InterruptedException {
        server2 = new PeerServerMock(kernelRule2.getKernel());
        server2.start();

        Node remoteNode = new Node(kernelRule1.getKernel().getConfig().p2pListenIp(),
                kernelRule1.getKernel().getConfig().p2pListenPort());

        KernelMock kernel2 = kernelRule2.getKernel();
        SemuxChannelInitializer ci = new SemuxChannelInitializer(kernelRule2.getKernel(), remoteNode);
        PeerClient client = kernelRule2.getKernel().getClient();
        client.connect(remoteNode, ci).sync();

        // waiting for the HELLO message to be sent
        await().until(() -> 1 == kernel2.getChannelManager().getActivePeers().size());

        client.close();
        await().until(() -> 0 == kernel2.getChannelManager().getActivePeers().size());
    }
}
