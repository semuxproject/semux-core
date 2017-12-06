/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.semux.Kernel;
import org.semux.KernelMock;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.DBFactory;

public class PeerServerMock {

    private KernelMock kernel;
    private PeerServer server;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private DBFactory dbFactory;

    public PeerServerMock(DBFactory dbFactory) {
        this.dbFactory = dbFactory;
    }

    public synchronized void start(String ip, int port) {
        if (isRunning.compareAndSet(false, true)) {
            new Thread(() -> {
                kernel = new KernelMock();

                kernel.setBlockchain(new BlockchainImpl(kernel.getConfig(), this.dbFactory));
                kernel.setClient(new PeerClient(ip, port, kernel.getCoinbase()));
                kernel.setChannelManager(new ChannelManager());
                kernel.setPendingManager(new PendingManager(kernel));
                kernel.setNodeManager(new NodeManager(kernel));

                kernel.setConsensus(new SemuxBFT(kernel));
                kernel.setSyncManager(new SemuxSync(kernel));

                server = new PeerServer(kernel);
                server.start(ip, port);
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
        if (isRunning.compareAndSet(true, false)) {
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

    public Kernel getKernel() {
        return kernel;
    }

    public PeerServer getServer() {
        return server;
    }
}
