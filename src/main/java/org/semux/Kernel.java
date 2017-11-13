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
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.xml.parsers.ParserConfigurationException;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.semux.api.ApiHandlerImpl;
import org.semux.api.SemuxAPI;
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
 * Kernel maintains global instances of different kernel modules. It contains
 * functionalities shared by both GUI and CLI.
 */
public class Kernel {
    private static Logger logger = LoggerFactory.getLogger(Config.class);

    public volatile boolean isRunning;

    private Wallet wallet;
    private EdDSA coinbase;

    private Blockchain chain;
    private PeerClient client;

    private PendingManager pendingMgr;
    private ChannelManager channelMgr;
    private NodeManager nodeMgr;

    private static Kernel instance;

    /**
     * Get the kernel instance.
     * 
     * @return
     */
    public static synchronized Kernel getInstance() {
        if (instance == null) {
            instance = new Kernel();
        }
        return instance;
    }

    private Kernel() {
    }

    /**
     * Initialize the kernel with the given arguments.
     * 
     * @param dataDir
     *            work directory
     * @param wallet
     *            wallet instance
     * @param coinbase
     *            coinbase account
     */
    public void init(String dataDir, Wallet wallet, int coinbase) {
        Config.DATA_DIR = dataDir;
        Config.init();

        this.wallet = wallet;
        this.coinbase = wallet.getAccounts().get(coinbase);
    }

    /**
     * Start the kernel.
     */
    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;

        // ====================================
        // initialization
        // ====================================
        logger.info(Config.getClientId(true));
        logger.info("System booting up: network = {}, coinbase = {}", Config.NETWORK_ID, coinbase);

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
                    throw new RuntimeException("Unexpected database: " + name);
                }
            }
        };
        chain = new BlockchainImpl(dbFactory);
        long number = chain.getLatestBlockNumber();
        logger.info("Latest block number = {}", number);

        String ip = SystemUtil.getIp();
        logger.info("Your IP address = {}", ip);
        client = new PeerClient(ip, Config.P2P_LISTEN_PORT, coinbase);

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

        Thread p2pThread = new Thread(() -> {
            p2p.start(Config.P2P_LISTEN_IP, Config.P2P_LISTEN_PORT);
        }, "p2p-main");
        p2pThread.start();

        // ====================================
        // start API module
        // ====================================
        SemuxAPI api = new SemuxAPI(new ApiHandlerImpl(wallet, chain, channelMgr, pendingMgr, nodeMgr, client));

        if (Config.API_ENABLED) {
            Thread apiThread = new Thread(() -> {
                api.start(Config.API_LISTEN_IP, Config.API_LISTEN_PORT);
            }, "api-main");
            apiThread.start();
        }

        // ====================================
        // start sync/consensus
        // ====================================
        SemuxSync sync = SemuxSync.getInstance();
        sync.init(chain, channelMgr);

        SemuxBFT cons = SemuxBFT.getInstance();
        cons.init(chain, channelMgr, pendingMgr, coinbase);

        Thread consThread = new Thread(() -> {
            cons.start();
        }, "cons");
        consThread.start();

        // ====================================
        // add port forwarding
        // ====================================
        new Thread(() -> {
            try {
                GatewayDiscover discover = new GatewayDiscover();
                Map<InetAddress, GatewayDevice> devices = discover.discover();
                for (InetAddress k : devices.keySet()) {
                    GatewayDevice gw = devices.get(k);
                    logger.info("Found a gateway device: local address = {}, external address = {}",
                            gw.getLocalAddress().getHostAddress(), gw.getExternalIPAddress());

                    gw.deletePortMapping(Config.P2P_LISTEN_PORT, "TCP");
                    gw.addPortMapping(Config.P2P_LISTEN_PORT, Config.P2P_LISTEN_PORT,
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
            isRunning = false;

            pendingMgr.stop();
            nodeMgr.stop();

            try {
                sync.stop();
                cons.stop();

                // make sure consensus thread is fully stopped
                consThread.join();
            } catch (InterruptedException e) {
                logger.error("Failed to stop sync/consensus properly");
            }

            // make sure no thread is updating state
            WriteLock lock = Config.STATE_LOCK.writeLock();
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
}
