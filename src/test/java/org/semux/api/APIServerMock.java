/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import org.junit.Assert;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;

public class APIServerMock {

    private SemuxAPI server;

    private boolean isRunning;

    public Blockchain chain;
    public ChannelManager channelMgr;
    public PendingManager pendingMgr;
    public PeerClient client;
    public NodeManager nodeMgr;

    public synchronized void start(String ip, int port) {
        if (!isRunning) {
            isRunning = true;

            new Thread(() -> {
                EdDSA coinbase = new EdDSA();

                chain = new BlockchainImpl(MemoryDB.FACTORY);
                channelMgr = new ChannelManager();
                pendingMgr = new PendingManager();
                client = new PeerClient("127.0.0.1", 5161, coinbase);
                nodeMgr = new NodeManager(chain, pendingMgr, channelMgr, client);

                server = new SemuxAPI(new APIHandler(chain, pendingMgr, channelMgr, nodeMgr, client));
                server.start(ip, port);
            }, "api").start();

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

            Assert.fail("Failed to start API server");
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

            Assert.fail("Failed to stop API server");
        }
    }
}
