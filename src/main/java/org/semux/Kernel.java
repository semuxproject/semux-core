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
import org.semux.config.MainNetConfig;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
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
import org.semux.net.SemuxChannelInitializer;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Kernel holds references to each individual components.
 */
public class Kernel {
    private static final Logger logger = LoggerFactory.getLogger(Kernel.class);

    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    private Config config = null;

    private Wallet wallet;
    private EdDSA coinbase;

    private Blockchain chain;
    private PeerClient client;

    private PendingManager pendingMgr;
    private ChannelManager channelMgr;
    private NodeManager nodeMgr;

    /**
     * Creates a kernel instance and initializes it.
     * 
     * @param dataDir
     *            work directory
     * @param wallet
     *            wallet instance
     * @param coinbase
     *            coinbase account
     */
    public Kernel(String dataDir, Wallet wallet, int coinbase) {
        this.config = new MainNetConfig(dataDir);

        this.wallet = wallet;
        this.coinbase = wallet.getAccounts().get(coinbase);
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
            private final KVDB indexDB = new LevelDB(DBName.INDEX);
            private final KVDB blockDB = new LevelDB(DBName.BLOCK);
            private final KVDB accountDB = new LevelDB(DBName.ACCOUNT);
            private final KVDB delegateDB = new LevelDB(DBName.DELEGATE);
            private final KVDB voteDB = new LevelDB(DBName.VOTE);

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
        chain = new BlockchainImpl(dbFactory);
        long number = chain.getLatestBlockNumber();
        logger.info("Latest block number = {}", number);

        String ip = config.p2pDeclaredIp().isPresent() ? config.p2pDeclaredIp().get() : SystemUtil.getIp();
        logger.info("Your IP address = {}", ip);
        client = new PeerClient(ip, config.p2pListenPort(), coinbase);

        // ====================================
        // start channel/pending/node manager
        // ====================================
        channelMgr = new ChannelManager();
        pendingMgr = new PendingManager(chain, channelMgr);
        nodeMgr = new NodeManager(chain, channelMgr, pendingMgr, client);

        pendingMgr.start();
        nodeMgr.start();

        // ====================================
        // start p2p module
        // ====================================
        SemuxChannelInitializer ci = new SemuxChannelInitializer(chain, channelMgr, pendingMgr, nodeMgr, client, null);
        PeerServer p2p = new PeerServer(ci);

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
        SemuxSync sync = SemuxSync.getInstance();
        sync.init(chain, channelMgr);

        SemuxBFT cons = SemuxBFT.getInstance();
        cons.init(chain, channelMgr, pendingMgr, coinbase);

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
    public PeerClient getPeerClient() {
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
}
