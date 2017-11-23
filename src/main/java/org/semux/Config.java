/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.semux.core.Unit;
import org.semux.crypto.Hash;
import org.semux.net.msg.MessageCode;
import org.semux.util.Bytes;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System configurations.
 *
 */
public class Config {

    private static Logger logger = LoggerFactory.getLogger(Config.class);

    public static boolean init() {
        File f = new File(DATA_DIR, "config" + File.separator + "semux.properties");
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(f)) {
            props.load(in);

            for (Object k : props.keySet()) {
                String name = (String) k;

                switch (name) {
                case "network":
                    NETWORK_ID = Byte.parseByte(props.getProperty(name));
                    break;
                case "minTransactionFee":
                    MIN_TRANSACTION_FEE = Long.parseLong(props.getProperty(name));
                    break;

                case "p2p.ip":
                    P2P_LISTEN_IP = props.getProperty(name);
                    break;
                case "p2p.port":
                    P2P_LISTEN_PORT = Integer.parseInt(props.getProperty(name));
                    break;
                case "p2p.seedNodes":
                    String[] nodes = props.getProperty(name).split(",");
                    for (String node : nodes) {
                        String[] tokens = node.trim().split(":");
                        if (tokens.length == 2) {
                            P2P_SEED_NODES.add(new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])));
                        }
                    }
                    break;

                case "net.blacklist":
                    String[] blacklist = props.getProperty(name).split(",");
                    for (String ip : blacklist) {
                        if (!(ip = ip.trim()).isEmpty()) {
                            NET_BLACKLIST.add(ip);
                        }
                    }
                    break;
                case "net.maxConnections":
                    NET_MAX_CONNECTIONS = Integer.parseInt(props.getProperty(name));
                    break;
                case "net.maxQueueSize":
                    NET_MAX_QUEUE_SIZE = Integer.parseInt(props.getProperty(name));
                    break;
                case "net.maxQueueRate":
                    NET_MAX_QUEUE_RATE = Integer.parseInt(props.getProperty(name));
                    break;

                case "api.enabled":
                    API_ENABLED = Boolean.parseBoolean(props.getProperty(name));
                    break;
                case "api.ip":
                    API_LISTEN_IP = props.getProperty(name);
                    break;
                case "api.port":
                    API_LISTEN_PORT = Integer.parseInt(props.getProperty(name));
                    break;
                case "api.username":
                    API_USERNAME = props.getProperty(name);
                    break;
                case "api.password":
                    API_PASSWORD = props.getProperty(name);
                    break;
                default:
                    logger.error("Unsupported option: {} = {}", name, props.getProperty(name));
                    break;
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to load config file: {}", f, e);
        }
        return false;
    }

    // =========================
    // General
    // =========================

    /**
     * Network id [0: main, 1: test, 2: dev].
     */
    public static byte NETWORK_ID = 2;

    /**
     * Work directory.
     */
    public static String DATA_DIR = ".";

    /**
     * Maximum number of transactions per block.
     */
    public static int MAX_BLOCK_SIZE = 5000;

    /**
     * Minimum transaction fee.
     */
    public static long MIN_TRANSACTION_FEE = 50 * Unit.MILLI_SEM;

    /**
     * Minimum delegate fee.
     */
    public static long DELEGATE_BURN_AMOUNT = 1000 * Unit.SEM;

    /**
     * Number of blocks before the next validator set refresh.
     */
    public static long VALIDATOR_REFRESH_RATE = 128;

    /**
     * Number of blocks in one day.
     */
    public static long BLOCKS_PER_DAY = 2 * 60 * 24;

    /**
     * Deadline of the next mandatory upgrade.
     */
    public static long MANDATORY_UPGRADE = 60 * BLOCKS_PER_DAY;

    /**
     * State lock to prevent state inconsistency.
     */
    public static ReentrantReadWriteLock STATE_LOCK = new ReentrantReadWriteLock();

    // =========================
    // Client
    // =========================

    /**
     * Name of this client.
     */
    public static String CLIENT_NAME = "Semux";

    /**
     * Version of this client.
     */
    public static String CLIENT_VERSION = "1.0.0-rc.3";

    // =========================
    // Crypto
    // =========================

    /**
     * Algorithm name for 256-bit hash.
     */
    public static String CRYPTO_H256_ALG = "BLAKE2B-256";

    // =========================
    // P2P
    // =========================

    /**
     * P2P protocol version.
     */
    public static short P2P_VERSION = 4;

    /**
     * P2P listening address.
     */
    public static String P2P_LISTEN_IP = "0.0.0.0";

    /**
     * P2P listening port.
     */
    public static int P2P_LISTEN_PORT = 5161;

    /**
     * Seed nodes for P2P.
     */
    public static Set<InetSocketAddress> P2P_SEED_NODES = new HashSet<>();

    // =========================
    // Network
    // =========================

    /**
     * IP black list.
     */
    public static Set<String> NET_BLACKLIST = new HashSet<>();

    /**
     * Maximum number of connections.
     */
    public static int NET_MAX_CONNECTIONS = 256;

    /**
     * Maximum message queue size.
     */
    public static int NET_MAX_QUEUE_SIZE = 4096;

    /**
     * Maximum message queue sending rate.
     */
    public static int NET_MAX_QUEUE_RATE = 1; // 1ms

    /**
     * Maximum frame size.
     */
    public static int NET_MAX_FRAME_SIZE = 128 * 1024;

    /**
     * Maximum packet size.
     */
    public static int NET_MAX_PACKET_SIZE = 8 * 1024 * 1024;

    /**
     * Timeout for peer connection.
     */
    public static int NET_TIMEOUT_CONNECT = 10 * 1000; // 10 seconds

    /**
     * Timeout for idle connection.
     */
    public static int NET_TIMEOUT_IDLE = 2 * 60 * 1000; // 2 minutes

    /**
     * Expire time of the handshake message.
     */
    public static int NET_HANDSHAKE_EXPIRE = 5 * 60 * 1000; // 5 minutes

    /**
     * Message broadcast redundancy.
     */
    public static int NET_RELAY_REDUNDANCY = 16;

    /**
     * Privileged message types
     */
    public static Set<MessageCode> PRIORITIZED_MESSAGES = new HashSet<>();
    static {
        PRIORITIZED_MESSAGES.add(MessageCode.BFT_NEW_HEIGHT);
        PRIORITIZED_MESSAGES.add(MessageCode.BFT_NEW_VIEW);
        PRIORITIZED_MESSAGES.add(MessageCode.BFT_PROPOSAL);
        PRIORITIZED_MESSAGES.add(MessageCode.BFT_VOTE);
    }

    // =========================
    // API
    // =========================

    /**
     * API enabled.
     */
    public static boolean API_ENABLED = false;

    /**
     * API listening address.
     */
    public static String API_LISTEN_IP = "127.0.0.1";

    /**
     * API listening port.
     */
    public static int API_LISTEN_PORT = 5171;

    /**
     * API basic authentication username.
     */
    public static String API_USERNAME = "";

    /**
     * API basic authentication password.
     */
    public static String API_PASSWORD = "";

    // =========================
    // BFT consensus
    // =========================

    /**
     * The duration of NEW_HEIGHT state. This allows validators to catch up.
     */
    public static long BFT_NEW_HEIGHT_TIMEOUT = 3000;

    /**
     * The duration of PREPROSE state.
     */
    public static long BFT_PROPOSE_TIMEOUT = 12000;

    /**
     * The duration of VALIDATE state.
     */
    public static long BFT_VALIDATE_TIMEOUT = 6000;

    /**
     * The duration of PRE_COMMIT state.
     */
    public static long BFT_PRE_COMMIT_TIMEOUT = 6000;

    /**
     * The duration of COMMIT state. May be skipped after +2/3 commit votes.
     */
    public static long BFT_COMMIT_TIMEOUT = 3000;

    /**
     * The duration of FINALIZE state. This allows validators to persist block.
     */
    public static long BFT_FINALIZE_TIMEOUT = 3000;

    // =========================
    // Virtual machine
    // =========================

    /**
     * Enable virtual machine.
     */
    public static boolean VM_ENABLED = false;

    /**
     * Maximum size of process stack.
     */
    public static int VM_STACK_SIZE_LIMIT = 64 * 1024;

    /**
     * Maximum size of process heap.
     */
    public static int VM_HEAP_SIZE_LIMIT = 4 * 1024 * 1024;

    // =========================
    // Extra
    // =========================

    /**
     * Returns the client id.
     * 
     * @return
     */
    public static String getClientId(boolean humanReadable) {
        if (humanReadable) {
            return String.format("%s v%s", CLIENT_NAME, CLIENT_VERSION);
        } else {
            return String.format("%s/v%s/%s/%s", CLIENT_NAME, CLIENT_VERSION, SystemUtil.getOSName(),
                    SystemUtil.getOSArch());
        }
    }

    /**
     * Returns block reward for the given block.
     * 
     * @param number
     *            block number
     * @return the block reward
     */
    public static long getBlockReward(long number) {
        if (number <= 75_000_000L) {
            return 1 * Unit.SEM;
        } else {
            return 0;
        }
    }

    /**
     * Returns the number of validators.
     * 
     * @param number
     * @return
     */
    public static int getNumberOfValidators(long number) {
        long step = 2 * 60 * 2;

        if (number < 48 * step) {
            return (int) (16 + number / step);
        } else {
            return 64;
        }
    }

    /**
     * Returns the primary validator for a specific [height, view].
     * 
     * @param validators
     * @param height
     * @param view
     * @return
     */
    public static String getPrimaryValidator(List<String> validators, long height, int view) {
        byte[] key = Bytes.merge(Bytes.of(height), Bytes.of(view));
        return validators.get((Hash.h256(key)[0] & 0xff) % validators.size());
    }

    /**
     * Returns whether this network is main net.
     * 
     * @return
     */
    public static boolean isMainNet() {
        return NETWORK_ID == 0;
    }

    /**
     * Returns whether this network is test net.
     * 
     * @return
     */
    public static boolean isTestNet() {
        return NETWORK_ID == 1;
    }

    /**
     * Returns whether this network is dev net.
     * 
     * @return
     */
    public static boolean isDevNet() {
        return NETWORK_ID == 2;
    }
}