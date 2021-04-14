/**
 * Copyright (c) 2017-2020 The Semux Developers
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
import org.semux.db.Database;
import org.semux.db.DatabaseFactory;
import org.semux.db.DatabaseName;
import org.semux.db.LeveldbDatabase.LeveldbFactory;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;

public class SemuxApiMock {

    private KernelMock kernel;
    private SemuxApiService server;

    private DatabaseFactory dbFactory;
    private PeerClient client;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public SemuxApiMock(KernelMock kernel) {
        this.kernel = kernel;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            Config config = kernel.getConfig();

            dbFactory = new LeveldbFactory(config.chainDir());
            client = new PeerClient(config.p2pListenIp(), config.p2pListenPort(), kernel.getCoinbase());

            kernel.setBlockchain(new BlockchainImpl(config, dbFactory));
            kernel.setChannelManager(new ChannelManager(kernel));
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setClient(client);
            kernel.setNodeManager(new NodeManager(kernel));

            server = new SemuxApiService(kernel);
            server.start(config.apiListenIp(), config.apiListenPort());
        }
    }

    public synchronized void stop() {
        if (isRunning.compareAndSet(true, false)) {
            server.stop();

            client.close();

            for (DatabaseName name : DatabaseName.values()) {
                Database db = dbFactory.getDB(name);
                db.close();
            }
        }
    }

    public KernelMock getKernel() {
        return kernel;
    }

    public SemuxApiService getApi() {
        return server;
    }
}
