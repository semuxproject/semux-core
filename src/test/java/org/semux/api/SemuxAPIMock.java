/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.atomic.AtomicBoolean;

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
            Config config = kernel.getConfig();

            kernel.setBlockchain(
                    new BlockchainImpl(config, new LevelDBFactory(config.dataDir())));
            kernel.setChannelManager(new ChannelManager(kernel));
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setClient(new PeerClient(config.p2pListenIp(), config.p2pListenPort(), kernel.getCoinbase()));
            kernel.setNodeManager(new NodeManager(kernel));

            server = new SemuxAPI(kernel);
            new Thread(() -> {
                server.start(config.apiListenIp(), config.apiListenPort());
            }, "api").start();

            await().until(() -> server.isRunning());
        }
    }

    public synchronized void stop() {
        if (isRunning.compareAndSet(true, false)) {
            server.stop();

            await().until(() -> !server.isRunning());
        }
    }

    public KernelMock getKernel() {
        return kernel;
    }
}
