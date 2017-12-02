/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.xml.parsers.ParserConfigurationException;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.semux.api.SemuxAPI;
import org.semux.config.Config;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Consensus;
import org.semux.core.PendingManager;
import org.semux.core.SyncManager;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.db.DBFactory;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB;
import org.semux.exception.KernelException;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.net.PeerServer;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Kernel holds references to each individual components.
 */
public class Kernel {
    protected static final Logger logger = LoggerFactory.getLogger(Kernel.class);

    protected AtomicBoolean isRunning = new AtomicBoolean(false);
    protected ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    protected Config config = null;

    protected Wallet wallet;
    protected EdDSA coinbase;

    protected Blockchain chain;
    protected PeerClient client;

    protected PendingManager pendingMgr;
    protected ChannelManager channelMgr;
    protected NodeManager nodeMgr;

    protected SemuxSync sync;
    protected SemuxBFT cons;

    /**
     * Creates a kernel instance and initializes it.
     * 
     * @param config
     *            the config instance
     * @param wallet
     *            the wallet instance
     * @param coinbase
     *            the coinbase key
     */
    public Kernel(Config config, Wallet wallet, EdDSA coinbase) {
        this.config = config;
        this.wallet = wallet;
        this.coinbase = coinbase;
    }

    /**
     * Start the kernel.
     */
    public void start() {
        if (isRunning()) {
            return;
        }
        isRunning.set(true);

        // ====================================
        // initialization
        // ====================================
        logger.info(config.getClientId());
        logger.info("System booting up: network = [{}, {}], coinbase = {}", config.networkId(), config.networkVersion(),
                coinbase);

        DBFactory dbFactory = new DBFactory() {
            private final KVDB indexDB = new LevelDB(config.dataDir(), DBName.INDEX);
            private final KVDB blockDB = new LevelDB(config.dataDir(), DBName.BLOCK);
            private final KVDB accountDB = new LevelDB(config.dataDir(), DBName.ACCOUNT);
            private final KVDB delegateDB = new LevelDB(config.dataDir(), DBName.DELEGATE);
            private final KVDB voteDB = new LevelDB(config.dataDir(), DBName.VOTE);

            @Override
            public KVDB getDB(DBName name) {
                switch (name) {
                case INDEX:
                    return indexDB;
                case BLOCK:
                    return blockDB;
                case ACCOUNT:
                    return accountDB;
                case DELEGATE:
                    return delegateDB;
                case VOTE:
                    return voteDB;
                default:
                    throw new KernelException("Unexpected database: " + name);
                }
            }
        };
        chain = new BlockchainImpl(this, dbFactory);
        long number = chain.getLatestBlockNumber();
        logger.info("Latest block number = {}", number);

        String ip = config.p2pDeclaredIp().isPresent() ? config.p2pDeclaredIp().get() : SystemUtil.getIp();
        logger.info("Your IP address = {}", ip);
        client = new PeerClient(ip, config.p2pListenPort(), coinbase);

        // ====================================
        // start channel/pending/node manager
        // ====================================
        channelMgr = new ChannelManager();
        pendingMgr = new PendingManager(this);
        nodeMgr = new NodeManager(this);

        pendingMgr.start();
        nodeMgr.start();

        // ====================================
        // start p2p module
        // ====================================
        PeerServer p2p = new PeerServer(this);

        Thread p2pThread = new Thread(() -> p2p.start(config.p2pListenIp(), config.p2pListenPort()), "p2p");
        p2pThread.start();

        // ====================================
        // start API module
        // ====================================
        SemuxAPI api = new SemuxAPI(this);

        if (config.apiEnabled()) {
            Thread apiThread = new Thread(() -> api.start(config.apiListenIp(), config.apiListenPort()), "api");
            apiThread.start();
        }

        // ====================================
        // start sync/consensus
        // ====================================
        sync = new SemuxSync(this);
        cons = new SemuxBFT(this);

        Thread consThread = new Thread(cons::start, "cons");
        consThread.start();

        // ====================================
        // add port forwarding
        // ====================================
        new Thread(() -> {
            try {
                GatewayDiscover discover = new GatewayDiscover();
                Map<InetAddress, GatewayDevice> devices = discover.discover();
                for (Map.Entry<InetAddress, GatewayDevice> entry : devices.entrySet()) {
                    GatewayDevice gw = entry.getValue();
                    logger.info("Found a gateway device: local address = {}, external address = {}",
                            gw.getLocalAddress().getHostAddress(), gw.getExternalIPAddress());

                    gw.deletePortMapping(config.p2pListenPort(), "TCP");
                    gw.addPortMapping(config.p2pListenPort(), config.p2pListenPort(),
                            gw.getLocalAddress().getHostAddress(), "TCP", "Semux P2P network");
                }
            } catch (IOException | SAXException | ParserConfigurationException e) {
                logger.info("Failed to add port mapping", e);
            }
        }, "upnp").start();

        // ====================================
        // register shutdown hook
        // ====================================
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning.set(false);

            pendingMgr.stop();
            nodeMgr.stop();

            try {
                sync.stop();
                cons.stop();

                // make sure consensus thread is fully stopped
                consThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // https://stackoverflow.com/a/4906814/670662
                logger.error("Failed to stop sync/consensus properly");
            }

            // make sure no thread is reading/writing the state
            WriteLock lock = stateLock.writeLock();
            lock.lock();
            for (DBName name : DBName.values()) {
                if (name != DBName.TEST) {
                    dbFactory.getDB(name).close();
                }
            }
            lock.unlock();

            api.stop();
            p2p.stop();
        }, "shutdown-hook"));
    }

    /**
     * Returns the wallet.
     * 
     * @return
     */
    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Returns the coinbase.
     * 
     * @return
     */
    public EdDSA getCoinbase() {
        return coinbase;
    }

    /**
     * Returns the blockchain.
     * 
     * @return
     */
    public Blockchain getBlockchain() {
        return chain;
    }

    /**
     * Returns the peer client.
     * 
     * @return
     */
    public PeerClient getClient() {
        return client;
    }

    /**
     * Returns the pending manager.
     * 
     * @return
     */
    public PendingManager getPendingManager() {
        return pendingMgr;
    }

    /**
     * Returns the channel manager.
     * 
     * @return
     */
    public ChannelManager getChannelManager() {
        return channelMgr;
    }

    /**
     * Returns the node manager.
     * 
     * @return
     */
    public NodeManager getNodeManager() {
        return nodeMgr;
    }

    /**
     * Returns whether the kernel is running
     *
     * @return
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Returns the config.
     * 
     * @return
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Returns the state lock.
     * 
     * @return
     */
    public ReentrantReadWriteLock getStateLock() {
        return stateLock;
    }

    /**
     * Returns the syncing manager.
     * 
     * @return
     */
    public SyncManager getSyncManager() {
        return sync;
    }

    /**
     * Returns the consensus.
     * 
     * @return
     */
    public Consensus getConsensus() {
        return cons;
    }
}
