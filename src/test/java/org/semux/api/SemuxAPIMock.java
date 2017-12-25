/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.semux.KernelMock;
import org.semux.config.Config;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.LevelDB.LevelDBFactory;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;

public class SemuxAPIMock {

    private KernelMock kernel;
    private SemuxAPI server;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public SemuxAPIMock(KernelMock kernel) {
        this.kernel = kernel;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            new Thread(() -> {
                Config config = kernel.getConfig();

                kernel.setBlockchain(
                        new BlockchainImpl(config, new LevelDBFactory(config.dataDir())));
                kernel.setChannelManager(new ChannelManager(kernel));
                kernel.setPendingManager(new PendingManager(kernel));
                kernel.setClient(new PeerClient(config.p2pListenIp(), config.p2pListenPort(), kernel.getCoinbase()));
                kernel.setNodeManager(new NodeManager(kernel));

                server = new SemuxAPI(kernel);
                server.start(config.apiListenIp(), config.apiListenPort());
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

    public KernelMock getKernel() {
        return kernel;
    }
}
