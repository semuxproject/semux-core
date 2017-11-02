package org.semux.net.msg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semux.Config;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.net.PeerServerMock;
import org.semux.net.SemuxChannelInitializer;
import org.semux.net.msg.p2p.PingMessage;
import org.semux.net.msg.p2p.PongMessage;

public class MessageQueueTest {

    private PeerClient remoteClient;
    private InetSocketAddress remoteAddress;

    private PeerServerMock server;

    @Before
    public void setup() {
        EdDSA key = new EdDSA();
        remoteClient = new PeerClient("127.0.0.1", 5161, key);
        remoteAddress = new InetSocketAddress(remoteClient.getIp(), remoteClient.getPort());

        server = new PeerServerMock();
        server.start(remoteClient, true);
    }

    private Channel connect() throws InterruptedException {
        EdDSA key = new EdDSA();
        PeerClient client = new PeerClient("127.0.0.1", 5162, key);

        Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
        ChannelManager channelMgr = new ChannelManager();
        PendingManager pendingMgr = new PendingManager(chain, channelMgr);
        NodeManager nodeMgr = new NodeManager(chain, channelMgr, pendingMgr, client);

        SemuxChannelInitializer ci = new SemuxChannelInitializer(chain, channelMgr, pendingMgr, nodeMgr, client,
                remoteAddress);
        client.connectAsync(remoteAddress, ci).sync();

        while (channelMgr.getActiveChannels().isEmpty()) {
            Thread.sleep(100);
        }
        return channelMgr.getActiveChannels().get(0);
    }

    @Test
    public void testQueueOverflow() throws InterruptedException {
        Channel ch = connect();

        PingMessage msg = new PingMessage();
        assertTrue(ch.getMessageQueue().sendMessage(msg));
        for (int i = 0; i < Config.NET_MAX_QUEUE_SIZE * 2; i++) {
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
