/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.net.Channel;
import org.semux.net.PeerClient;
import org.semux.net.PeerServerMock;
import org.semux.net.SemuxChannelInitializer;
import org.semux.net.msg.p2p.PingMessage;
import org.semux.net.msg.p2p.PongMessage;
import org.semux.rules.KernelRule;

public class MessageQueueTest {

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

    private Channel connect() throws InterruptedException {
        server2 = new PeerServerMock(kernelRule2.getKernel());
        server2.start();

        KernelMock kernel2 = kernelRule2.getKernel();
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1",
                kernelRule1.getKernel().getConfig().p2pListenPort());
        SemuxChannelInitializer ci = new SemuxChannelInitializer(kernelRule2.getKernel(), remoteAddress);
        PeerClient client = kernelRule2.getKernel().getClient();
        client.connectAsync(remoteAddress, ci).sync();

        while (kernel2.getChannelManager().getActiveChannels().isEmpty()) {
            Thread.sleep(100);
        }
        return kernel2.getChannelManager().getActiveChannels().get(0);
    }

    @Test
    public void testQueueOverflow() throws InterruptedException {
        Channel ch = connect();

        PingMessage msg = new PingMessage();
        assertTrue(ch.getMessageQueue().sendMessage(msg));
        for (int i = 0; i < server1.getKernel().getConfig().netMaxMessageQueueSize() * 2; i++) {
            ch.getMessageQueue().sendMessage(msg);
        }
        assertFalse(ch.getMessageQueue().sendMessage(msg));

        Thread.sleep(200);
        assertFalse(ch.isActive());
    }

    @Test
    public void testSendRequest() throws InterruptedException {
        Channel ch = connect();

        PingMessage msg = new PingMessage();
        ch.getMessageQueue().sendMessage(msg);

        Thread.sleep(200);
        assertTrue(ch.getMessageQueue().isIdle());
        assertTrue(ch.isActive());
    }

    @Test
    public void testSendResponse() throws InterruptedException {
        Channel ch = connect();

        PongMessage msg = new PongMessage();
        ch.getMessageQueue().sendMessage(msg);

        Thread.sleep(200);
        assertTrue(ch.getMessageQueue().isIdle());
        assertTrue(ch.isActive());
    }
}
