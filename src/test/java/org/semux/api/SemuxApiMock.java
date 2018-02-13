/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.concurrent.atomic.AtomicBoolean;

import org.semux.KernelMock;
import org.semux.api.http.SemuxApiService;
import org.semux.config.Config;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.db.Db;
import org.semux.db.DbFactory;
import org.semux.db.DbName;
import org.semux.db.LevelDb.LevelDbFactory;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;

public class SemuxApiMock {

    private KernelMock kernel;
    private SemuxApiService server;

    private DbFactory dbFactory;
    private PeerClient client;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public SemuxApiMock(KernelMock kernel) {
        this.kernel = kernel;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            Config config = kernel.getConfig();

            dbFactory = new LevelDbFactory(config.databaseDir());
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

            for (DbName name : DbName.values()) {
                Db db = dbFactory.getDB(name);
                db.close();
            }
        }
    }

    public KernelMock getKernel() {
        return kernel;
    }
}
