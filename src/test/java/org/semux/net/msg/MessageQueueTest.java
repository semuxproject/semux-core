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
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.crypto.EdDSA;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.net.PeerServerMock;
import org.semux.net.SemuxChannelInitializer;
import org.semux.net.msg.p2p.PingMessage;
import org.semux.net.msg.p2p.PongMessage;
import org.semux.rules.TemporaryDBRule;

public class MessageQueueTest {

    private static final String P2P_IP = "127.0.0.1";
    private static final int P2P_PORT = 15161;

    private PeerServerMock server;

    @Rule
    public TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    @Before
    public void setup() {
        server = new PeerServerMock(temporaryDBFactory);
        server.start(P2P_IP, P2P_PORT);
        assertTrue(server.getServer().isListening());
    }

    private Channel connect() throws InterruptedException {
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

        InetSocketAddress remoteAddress = new InetSocketAddress(P2P_IP, P2P_PORT);
        SemuxChannelInitializer ci = new SemuxChannelInitializer(kernel, remoteAddress);
        client.connectAsync(remoteAddress, ci).sync();

        while (kernel.getChannelManager().getActiveChannels().isEmpty()) {
            Thread.sleep(100);
        }
        return kernel.getChannelManager().getActiveChannels().get(0);
    }

    @Test
    public void testQueueOverflow() throws InterruptedException {
        Channel ch = connect();

        PingMessage msg = new PingMessage();
        assertTrue(ch.getMessageQueue().sendMessage(msg));
        for (int i = 0; i < server.getKernel().getConfig().netMaxMessageQueueSize() * 2; i++) {
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

    @After
    public void teardown() {
        server.stop();
    }
}
