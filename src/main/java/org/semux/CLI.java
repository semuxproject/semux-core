/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.util.List;

import org.semux.api.APIHandler;
import org.semux.api.SemuxAPI;
import org.semux.consensus.SemuxBFT;
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
import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The command line interface.
 *
 */
public class CLI {

    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    private static String password = null;
    private static int coinbaseIndex = 0;
    private static String dataDir = ".";

    private static void printUsage() {
        System.out.println("===============================================================");
        System.out.println(Config.CLIENT_FULL_NAME);
        System.out.println("===============================================================\n");
        System.out.println("Usage:");
        System.out.println("  ./run.sh [options] or run.bat [options]\n");
        System.out.println("Options:");
        System.out.println("  -h, --help              Print help info and exit");
        System.out.println("  -v, --version           Show the version of this client");
        System.out.println("  -d, --datadir   path    Specify the data directory");
        System.out.println("  -a, --account   create  Create an new account and exit");
        System.out.println("                  list    List all accounts and exit");
        System.out.println("  -c, --coinbase  index   Specify which account to be used as coinbase");
        System.out.println("  -p, --password  pwd     Password of the wallet");
        System.out.println();
    }

    private static void parseArguments(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                case "-h":
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
                case "-v":
                case "--version":
                    System.out.println(Config.CLIENT_VERSION);
                    System.exit(0);
                    break;
                case "-p":
                case "--password":
                    password = args[++i];
                    break;
                case "-c":
                case "--coinbase":
                    coinbaseIndex = Integer.parseInt(args[++i]);
                    break;
                case "-a":
                case "--account":
                    String type = args[++i];
                    switch (type) {
                    case "create":
                        Wallet.doCreate(password);
                        break;
                    case "list":
                        Wallet.doList(password);
                        break;
                    default:
                        printUsage();
                        break;
                    }
                    System.exit(0);
                    break;
                case "-d":
                case "--datadir":
                    dataDir = args[++i];
                    break;
                default:
                    printUsage();
                    System.exit(-1);
                    break;
                }
            }
        } catch (Exception e) {
            printUsage();
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        parseArguments(args);
        Config.DATA_DIR = dataDir;
        Config.init();

        // ====================================
        // unlock wallet
        // ====================================
        if (password == null) {
            password = SystemUtil.readPassword();
        }

        Wallet wallet = Wallet.getInstance();
        if (!wallet.unlock(password)) {
            logger.error("Failed to unlock wallet");
            System.exit(-1);
        }

        List<EdDSA> accounts = wallet.getAccounts();
        if (accounts.isEmpty()) {
            EdDSA key = new EdDSA();
            wallet.addAccount(key);
            wallet.flush();
            accounts = wallet.getAccounts();
            logger.info("A new account has been created: address = {}", key.toAddressString());
        }

        if (coinbaseIndex < 0 || coinbaseIndex >= accounts.size()) {
            logger.error("Coinbase does not exist");
            System.exit(-1);
        }
        EdDSA coinbase = accounts.get(coinbaseIndex);

        // ====================================
        // initialization
        // ====================================
        logger.info("Client: {}", Config.CLIENT_FULL_NAME);
        logger.info("System booting up: network = {}, coinbase = {}", Config.NETWORK, coinbase);

        Blockchain chain = new BlockchainImpl(new DBFactory() {
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
        });
        long height = chain.getLatestBlockNumber();
        String nextFork = chain.getGenesis().getConfig().get("nextUpgrade").toString();
        if (height >= Long.parseLong(nextFork)) {
            logger.error("Please upgrade your client");
            System.exit(-1);
        } else {
            logger.info("Latest block number = {}", height);
        }

        // ====================================
        // start channel/node/pending manager
        // ====================================
        ChannelManager channelMgr = new ChannelManager();

        PeerClient client = new PeerClient(SystemUtil.getIp(), Config.P2P_LISTEN_PORT, coinbase);
        NodeManager nodeMgr = new NodeManager(chain, channelMgr, client);
        nodeMgr.start();

        PendingManager pendingMgr = PendingManager.getInstance();
        pendingMgr.start(chain, channelMgr);

        chain.addListener(pendingMgr);

        // ====================================
        // start p2p module
        // ====================================
        PeerServer p2p = new PeerServer(new SemuxChannelInitializer(chain, channelMgr, nodeMgr, client, null));

        new Thread(() -> {
            p2p.start(Config.P2P_LISTEN_IP, Config.P2P_LISTEN_PORT);
        }, "p2p").start();

        // ====================================
        // start API module
        // ====================================
        SemuxAPI api = new SemuxAPI(new APIHandler(chain, channelMgr, pendingMgr, nodeMgr, client));

        new Thread(() -> {
            api.start(Config.API_LISTEN_IP, Config.API_LISTEN_PORT);
        }, "api").start();

        // ====================================
        // start consensus
        // ====================================
        SemuxBFT consensus = SemuxBFT.getInstance();
        consensus.init(chain, channelMgr, pendingMgr, coinbase);

        new Thread(() -> {
            consensus.start();
        }, "cons").start();

        // ====================================
        // register shutdown hook
        // ====================================
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consensus.stop();
            api.stop();
            p2p.stop();
        }, "shutdown-hook"));
    }

}
