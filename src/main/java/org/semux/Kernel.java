package org.semux;

import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.semux.api.APIHandler;
import org.semux.api.SemuxAPI;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Consensus;
import org.semux.core.PendingManager;
import org.semux.core.Sync;
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
import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kernel maintains global instances of different kernel modules. It contains
 * functionalities shared by both GUI and CLI.
 */
public class Kernel {
    private static Logger logger = LoggerFactory.getLogger(Config.class);

    private boolean isRunning;

    private Wallet wallet;
    private EdDSA coinbase;

    private Blockchain chain;
    private PeerClient client;

    private PendingManager pendingMgr;
    private ChannelManager channelMgr;
    private NodeManager nodeMgr;

    private SemuxSync sync;
    private SemuxBFT cons;

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

        // ====================================
        // initialization
        // ====================================
        logger.info("Client: {}", Config.CLIENT_FULL_NAME);
        logger.info("System booting up: network = {}, coinbase = {}", Config.NETWORK_ID, coinbase);

        DBFactory dbFactory = new DBFactory() {
            private final KVDB indexDB = new LevelDB(DBName.INDEX);
            private final KVDB blockDB = new LevelDB(DBName.BLOCK);
            private final KVDB accountDB = new LevelDB(DBName.ACCOUNT);
            private final KVDB delegateDB = new LevelDB(DBName.DELEGATE);
            private final KVDB voteDB = new LevelDB(DBName.VOTE);
            private final KVDB testDB = new LevelDB(DBName.TEST);

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
                case TEST:
                    return testDB;
                default:
                    throw new RuntimeException("Unexpected database: " + name);
                }
            }
        };
        chain = new BlockchainImpl(dbFactory);

        long height = chain.getLatestBlockNumber();
        String nextUpgrade = chain.getGenesis().getConfig().get("nextUpgrade").toString();
        if (height >= Long.parseLong(nextUpgrade)) {
            logger.error("Please upgrade your client");
            System.exit(-1);
        } else {
            logger.info("Latest block number = {}", height);
        }

        client = new PeerClient(SystemUtil.getIp(), Config.P2P_LISTEN_PORT, coinbase);

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
        }, "p2p");
        p2pThread.start();

        // ====================================
        // start API module
        // ====================================
        SemuxAPI api = new SemuxAPI(new APIHandler(wallet, chain, channelMgr, pendingMgr, nodeMgr, client));

        if (Config.API_ENABLED) {
            Thread apiThread = new Thread(() -> {
                api.start(Config.API_LISTEN_IP, Config.API_LISTEN_PORT);
            }, "api");
            apiThread.start();
        }

        // ====================================
        // start sync/consensus
        // ====================================
        sync = SemuxSync.getInstance();
        sync.init(chain, channelMgr);

        cons = SemuxBFT.getInstance();
        cons.init(chain, channelMgr, pendingMgr, coinbase);

        Thread consThread = new Thread(() -> {
            cons.start();
        }, "cons");
        consThread.start();

        // ====================================
        // register shutdown hook
        // ====================================
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
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
                dbFactory.getDB(name).close();
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
     * Returns the consensus manager.
     * 
     * @return
     */
    public Consensus getConsenus() {
        return cons;
    }

    /**
     * Return the sync manager.
     * 
     * @return
     */
    public Sync getSync() {
        return sync;
    }
}
