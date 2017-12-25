/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.concurrent.atomic.AtomicBoolean;

import org.semux.KernelMock;
import org.semux.config.Config;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.DBFactory;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB.LevelDBFactory;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;

public class SemuxAPIMock {

    private KernelMock kernel;
    private SemuxAPI server;
    private DBFactory dbFactory;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public SemuxAPIMock(KernelMock kernel) {
        this.kernel = kernel;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            Config config = kernel.getConfig();

            dbFactory = new LevelDBFactory(config.dataDir());
            kernel.setBlockchain(new BlockchainImpl(config, dbFactory));
            kernel.setChannelManager(new ChannelManager(kernel));
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setClient(new PeerClient(config.p2pListenIp(), config.p2pListenPort(), kernel.getCoinbase()));
            kernel.setNodeManager(new NodeManager(kernel));

            server = new SemuxAPI(kernel);
            server.start(config.apiListenIp(), config.apiListenPort());
        }
    }

    public synchronized void stop() {
        if (isRunning.compareAndSet(true, false)) {
            server.stop();

            for (DBName name : DBName.values()) {
                KVDB db = dbFactory.getDB(name);
                db.close();
            }
        }
    }

    public KernelMock getKernel() {
        return kernel;
    }
}
