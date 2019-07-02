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
import org.semux.Network;
import org.semux.config.Config;
import org.semux.consensus.SemuxBft;
import org.semux.consensus.SemuxSync;
import org.semux.core.BlockchainFactory;
import org.semux.core.Genesis;
import org.semux.core.PendingManager;
import org.semux.db.Database;
import org.semux.db.DatabaseFactory;
import org.semux.db.DatabaseName;
import org.semux.db.LeveldbDatabase.LeveldbFactory;

public class PeerServerMock {

    private KernelMock kernel;
    private PeerServer server;

    private DatabaseFactory dbFactory;
    private PeerClient client;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public PeerServerMock(KernelMock kernel) {
        this.kernel = kernel;
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            Config config = kernel.getConfig();

            dbFactory = new LeveldbFactory(config.databaseDir());
            client = new PeerClient(config.p2pListenIp(), config.p2pListenPort(), kernel.getCoinbase());

            kernel.setBlockchain(
                    new BlockchainFactory(config, Genesis.load(Network.DEVNET), dbFactory).getBlockchainInstance());
            kernel.setClient(client);
            kernel.setChannelManager(new ChannelManager(kernel));
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setNodeManager(new NodeManager(kernel));

            kernel.setBftManager(new SemuxBft(kernel));
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

            for (DatabaseName name : DatabaseName.values()) {
                Database db = dbFactory.getDB(name);
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
