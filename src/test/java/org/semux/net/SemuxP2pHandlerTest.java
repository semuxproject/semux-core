/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.Config;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.net.SemuxChannelInitializer;

public class SemuxP2pHandlerTest {

    private static PeerClient remoteClient;
    private static InetSocketAddress remoteAddress;

    private static PeerServerMock server;

    @BeforeClass
    public static void setup() {
        EdDSA key = new EdDSA();
        PeerClient remoteClient = new PeerClient("127.0.0.1", 5161, key);
        remoteAddress = new InetSocketAddress(remoteClient.getIp(), remoteClient.getPort());

        server = new PeerServerMock();
        server.start(remoteClient, true);
    }

    @Test
    public void testMaxConnections() throws InterruptedException {
        int prev = Config.NET_MAX_CONNECTIONS;
        try {
            for (int i = 0; i < 2; i++) {
                Config.NET_MAX_CONNECTIONS = i;

                EdDSA key = new EdDSA();
                PeerClient client = new PeerClient("127.0.0.1", 5162, key);

                Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
                ChannelManager channelMgr = new ChannelManager();
                PendingManager pendingMgr = new PendingManager(chain, channelMgr);
                NodeManager nodeMgr = new NodeManager(chain, channelMgr, pendingMgr, client);

                SemuxChannelInitializer ci = new SemuxChannelInitializer(chain, channelMgr, pendingMgr, nodeMgr, client,
                        remoteAddress);
                client.connectAsync(remoteAddress, ci).sync();

                // wait for handshake
                Thread.sleep(1000);
                assertEquals(i, channelMgr.getActivePeers().size());
            }
        } finally {
            Config.NET_MAX_CONNECTIONS = prev;
        }
    }

    @AfterClass
    public static void teardown() {
        server.stop();
    }
}
