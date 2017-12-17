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
import org.semux.db.LevelDB.LevelDBFactory;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.net.PeerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Kernel holds references to each individual components.
 */
public class Kernel {
    protected static final Logger logger = LoggerFactory.getLogger(Kernel.class);

    protected final AtomicBoolean isRunning = new AtomicBoolean(false);

    protected ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    protected Config config = null;

    protected Wallet wallet;
    protected EdDSA coinbase;

    protected Blockchain chain;
    protected PeerClient client;

    protected ChannelManager channelMgr;
    protected PendingManager pendingMgr;
    protected NodeManager nodeMgr;

    protected PeerServer p2p;
    protected SemuxAPI api;

    protected Thread consThread;
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
    public synchronized void start() {
        if (isRunning.get()) {
            return;
        }

        // ====================================
        // initialization
        // ====================================
        logger.info(config.getClientId());
        logger.info("System booting up: network = [{}, {}], coinbase = {}", config.networkId(), config.networkVersion(),
                coinbase);

        DBFactory dbFactory = new LevelDBFactory(config.dataDir());
        chain = new BlockchainImpl(config, dbFactory);
        long number = chain.getLatestBlockNumber();
        logger.info("Latest block number = {}", number);

        // ====================================
        // set up client
        // ====================================
        client = new PeerClient(config, coinbase);

        // ====================================
        // start channel/pending/node manager
        // ====================================
        channelMgr = new ChannelManager(this);
        pendingMgr = new PendingManager(this);
        nodeMgr = new NodeManager(this);

        pendingMgr.start();
        nodeMgr.start();

        // ====================================
        // start p2p module
        // ====================================
        p2p = new PeerServer(this);
        new Thread(p2p::start, "p2p").start();

        // ====================================
        // start API module
        // ====================================
        api = new SemuxAPI(this);
        if (config.apiEnabled()) {
            new Thread(api::start, "api").start();
        }

        // ====================================
        // start sync/consensus
        // ====================================
        sync = new SemuxSync(this);
        cons = new SemuxBFT(this);

        consThread = new Thread(cons::start, "cons");
        consThread.start();

        // ====================================
        // add port forwarding
        // ====================================
        new Thread(this::setupUpnp, "upnp").start();

        // ====================================
        // register shutdown hook
        // ====================================
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "shutdown-hook"));

        // set flag
        isRunning.set(true);
    }

    /**
     * Sets up uPnP port mapping.
     */
    protected void setupUpnp() {
        try {
            GatewayDiscover discover = new GatewayDiscover();
            Map<InetAddress, GatewayDevice> devices = discover.discover();
            for (Map.Entry<InetAddress, GatewayDevice> entry : devices.entrySet()) {
                GatewayDevice gw = entry.getValue();
                logger.info("Found a gateway device: local address = {}, external address = {}",
                        gw.getLocalAddress().getHostAddress(), gw.getExternalIPAddress());

                gw.deletePortMapping(config.p2pListenPort(), "TCP");
                gw.addPortMapping(config.p2pListenPort(), config.p2pListenPort(), gw.getLocalAddress().getHostAddress(),
                        "TCP", "Semux P2P network");
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            logger.info("Failed to add port mapping", e);
        }
    }

    /**
     * Stops the kernel.
     */
    public synchronized void stop() {
        if (!isRunning.get()) {
            return;
        }

        // stop consensus
        try {
            sync.stop();
            cons.stop();

            // make sure consensus thread is fully stopped
            consThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to stop sync/consensus properly");
        }

        // stop API and p2p
        api.stop();
        p2p.stop();

        // stop pending manager and node manager
        pendingMgr.stop();
        nodeMgr.stop();

        // close client
        client.close();

        // set flag
        isRunning.set(false);
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

    /**
     * Get instance of Semux API server
     *
     * @return API server
     */
    public SemuxAPI getApi() {
        return api;
    }

    /**
     * Returns the p2p server instance.
     *
     * @return a {@link PeerServer} instance or null
     */
    public PeerServer getP2p() {
        return p2p;
    }
}
