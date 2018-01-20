/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.util.concurrent.atomic.AtomicBoolean;

import org.semux.Kernel;
import org.semux.KernelMock;
import org.semux.config.Config;
import org.semux.consensus.SemuxBft;
import org.semux.consensus.SemuxSync;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.Db;
import org.semux.db.DbFactory;
import org.semux.db.DbName;
import org.semux.db.LevelDb.LevelDbFactory;

public class PeerServerMock {

    private KernelMock kernel;
    private PeerServer server;

    private DbFactory dbFactory;
    private PeerClient client;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public PeerServerMock(KernelMock kernel) {
        this.kernel = kernel;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            Config config = kernel.getConfig();

            dbFactory = new LevelDbFactory(config.dataDir());
            client = new PeerClient(config.p2pListenIp(), config.p2pListenPort(), kernel.getCoinbase());

            kernel.setBlockchain(new BlockchainImpl(config, dbFactory));
            kernel.setClient(client);
            kernel.setChannelManager(new ChannelManager(kernel));
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setNodeManager(new NodeManager(kernel));

            kernel.setConsensus(new SemuxBft(kernel));
            kernel.setSyncManager(new SemuxSync(kernel));

            // start peer server
            server = new PeerServer(kernel);
            server.start(config.p2pListenIp(), config.p2pListenPort());
        }
    }

    public synchronized void stop() {
        if (isRunning.compareAndSet(true, false)) {
            server.stop();

            client.close();

            for (DbName name : DbName.values()) {
                Db db = dbFactory.getDB(name);
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
