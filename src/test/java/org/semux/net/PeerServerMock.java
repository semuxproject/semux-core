/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import org.junit.Assert;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.MemoryDB;

public class PeerServerMock {

    private PeerServer server;

    private volatile boolean isRunning;

    public synchronized void start(PeerClient client, boolean isDiscoveryMode) {
        if (!isRunning) {
            isRunning = true;

            new Thread(() -> {
                Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
                ChannelManager channelMgr = new ChannelManager();
                PendingManager pendingMgr = new PendingManager(chain, channelMgr);
                NodeManager nodeMgr = new NodeManager(chain, channelMgr, pendingMgr, client);
                SemuxChannelInitializer ci = new SemuxChannelInitializer(chain, channelMgr, pendingMgr, nodeMgr, client,
                        null);

                ci.setDiscoveryMode(isDiscoveryMode);

                server = new PeerServer(ci);
                server.start(client.getIp(), client.getPort());
            }, "p2p-server").start();

            long timestamp = System.currentTimeMillis();
            while (System.currentTimeMillis() - timestamp < 30000) {
                if (server == null || !server.isListening()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    return;
                }
            }

            Assert.fail("Failed to start server");
        }
    }

    public synchronized void stop() {
        if (isRunning) {
            server.stop();

            long timestamp = System.currentTimeMillis();
            while (System.currentTimeMillis() - timestamp < 30000) {
                if (server.isListening()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    return;
                }
            }

            Assert.fail("Failed to stop server");
        }
    }

    public PeerServer getServer() {
        return server;
    }
}
