/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.semux.Kernel;
import org.semux.KernelMock;
import org.semux.config.Constants;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.MemoryDB.MemoryDBFactory;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;

public class SemuxAPIMock {

    private KernelMock kernel;
    private SemuxAPI server;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public synchronized void start(String ip, int port) {
        if (isRunning.compareAndSet(false, true)) {
            new Thread(() -> {
                kernel = new KernelMock();
                kernel.setBlockchain(new BlockchainImpl(kernel.getConfig(), new MemoryDBFactory()));
                kernel.setChannelManager(new ChannelManager());
                kernel.setPendingManager(new PendingManager(kernel));
                kernel.setClient(new PeerClient("127.0.0.1", Constants.DEFAULT_P2P_PORT, kernel.getCoinbase()));
                kernel.setNodeManager(new NodeManager(kernel));

                server = new SemuxAPI(kernel);
                server.start(ip, port);
            }, "api").start();

            long timestamp = System.currentTimeMillis();
            while (System.currentTimeMillis() - timestamp < 30000) {
                if (server == null || !server.isRunning()) {
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
        if (isRunning.compareAndSet(true, false)) {
            server.stop();

            long timestamp = System.currentTimeMillis();
            while (System.currentTimeMillis() - timestamp < 30000) {
                if (server.isRunning()) {
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

    public Kernel getKernel() {
        return kernel;
    }
}
