/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.rules.KernelRule;

public class PeerClientTest {

    private PeerServerMock server1;
    private PeerServerMock server2;

    @Rule
    public KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Rule
    public KernelRule kernelRule2 = new KernelRule(51620, 51720);

    @Before
    public void setup() {
        server1 = new PeerServerMock(kernelRule1.getKernel());
        server1.start();
        assertTrue(server1.getServer().isRunning());
    }

    @After
    public void teardown() {
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

        KernelMock kernel2 = kernelRule2.getKernel();
        InetSocketAddress remoteAddress = new InetSocketAddress(kernelRule1.getKernel().getConfig().p2pListenIp(),
                kernelRule1.getKernel().getConfig().p2pListenPort());
        SemuxChannelInitializer ci = new SemuxChannelInitializer(kernelRule2.getKernel(), remoteAddress);
        PeerClient client = kernelRule2.getKernel().getClient();
        client.connectAsync(remoteAddress, ci).sync();

        // waiting for the HELLO message to be sent
        Thread.sleep(1000);
        assertEquals(1, kernel2.getChannelManager().getActivePeers().size());

        client.close();
        assertEquals(0, kernel2.getChannelManager().getActivePeers().size());
    }
}
