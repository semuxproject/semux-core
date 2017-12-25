/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.util.concurrent.atomic.AtomicBoolean;

import org.semux.Kernel;
import org.semux.KernelMock;
import org.semux.config.Config;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.DBFactory;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB.LevelDBFactory;

public class PeerServerMock {

    private KernelMock kernel;
    private PeerServer server;
    private DBFactory dbFactory;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public PeerServerMock(KernelMock kernel) {
        this.kernel = kernel;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            Config config = kernel.getConfig();

            dbFactory = new LevelDBFactory(config.dataDir());
            kernel.setBlockchain(new BlockchainImpl(config, dbFactory));
            kernel.setClient(new PeerClient(config.p2pListenIp(), config.p2pListenPort(), kernel.getCoinbase()));
            kernel.setChannelManager(new ChannelManager(kernel));
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setNodeManager(new NodeManager(kernel));

            kernel.setConsensus(new SemuxBFT(kernel));
            kernel.setSyncManager(new SemuxSync(kernel));

            // start peer server
            server = new PeerServer(kernel);
            server.start(config.p2pListenIp(), config.p2pListenPort());
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

    public Kernel getKernel() {
        return kernel;
    }

    public PeerServer getServer() {
        return server;
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
