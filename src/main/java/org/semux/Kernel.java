/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.ParserConfigurationException;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.semux.api.http.SemuxApiService;
import org.semux.config.Config;
import org.semux.consensus.SemuxBft;
import org.semux.consensus.SemuxSync;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Consensus;
import org.semux.core.PendingManager;
import org.semux.core.SyncManager;
import org.semux.core.Wallet;
import org.semux.crypto.Key;
import org.semux.db.DbFactory;
import org.semux.db.DbName;
import org.semux.db.LevelDb.LevelDbFactory;
import org.semux.net.ChannelManager;
import org.semux.net.NodeManager;
import org.semux.net.PeerClient;
import org.semux.net.PeerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

/**
 * Kernel holds references to each individual components.
 */
public class Kernel {

    // Fix JNA issue: There is an incompatible JNA native library installed
    static {
        System.setProperty("jna.nosys", "true");
    }

    protected static final Logger logger = LoggerFactory.getLogger(Kernel.class);

    public enum State {
        STOPPED, BOOTING, RUNNING, STOPPING
    }

    protected State state = State.STOPPED;

    protected ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    protected Config config = null;

    protected Wallet wallet;
    protected Key coinbase;

    protected DbFactory dbFactory;
    protected Blockchain chain;
    protected PeerClient client;

    protected ChannelManager channelMgr;
    protected PendingManager pendingMgr;
    protected NodeManager nodeMgr;

    protected PeerServer p2p;
    protected SemuxApiService api;

    protected Thread consThread;
    protected SemuxSync sync;
    protected SemuxBft cons;

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
    public Kernel(Config config, Wallet wallet, Key coinbase) {
        this.config = config;
        this.wallet = wallet;
        this.coinbase = coinbase;
    }

    /**
     * Start the kernel.
     */
    public synchronized void start() {
        if (state != State.STOPPED) {
            return;
        } else {
            state = State.BOOTING;
        }

        // ====================================
        // initialization
        // ====================================
        logger.info(config.getClientId());
        logger.info("System booting up: network = {}, networkVersion = {}, coinbase = {}", config.network(),
                config.networkVersion(),
                coinbase);
        printSystemInfo();

        dbFactory = new LevelDbFactory(config.dataDir());
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
        p2p.start();

        // ====================================
        // start API module
        // ====================================
        api = new SemuxApiService(this);
        if (config.apiEnabled()) {
            api.start();
        }

        // ====================================
        // start sync/consensus
        // ====================================
        sync = new SemuxSync(this);
        cons = new SemuxBft(this);

        consThread = new Thread(cons::start, "cons");
        consThread.start();

        // ====================================
        // add port forwarding
        // ====================================
        new Thread(this::setupUpnp, "upnp").start();

        // ====================================
        // register shutdown hook
        // ====================================
        Launcher.registerShutdownHook("kernel", this::stop);

        state = State.RUNNING;
    }

    /**
     * Prints system info.
     */
    protected void printSystemInfo() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        // computer system
        ComputerSystem cs = hal.getComputerSystem();
        logger.info("Computer: manufacturer = {}, model = {}", cs.getManufacturer(), cs.getModel());

        // operating system
        OperatingSystem os = si.getOperatingSystem();
        logger.info("OS: name = {}", os);

        // cpu
        CentralProcessor cp = hal.getProcessor();
        logger.info("CPU: processor = {}, cores = {} / {}", cp, cp.getPhysicalProcessorCount(),
                cp.getLogicalProcessorCount());

        // memory
        GlobalMemory m = hal.getMemory();
        long mb = 1024L * 1024L;
        logger.info("Memory: total = {} MB, available = {} MB, swap total = {} MB, swap available = {} MB",
                m.getTotal() / mb,
                m.getAvailable() / mb,
                m.getSwapTotal() / mb,
                (m.getSwapTotal() - m.getSwapUsed()) / mb);

        // disk
        for (HWDiskStore disk : hal.getDiskStores()) {
            logger.info("Disk: name = {}, size = {} MB", disk.getName(), disk.getSize() / mb);
        }

        // network
        for (NetworkIF net : hal.getNetworkIFs()) {
            logger.info("Network: name = {}, ip = [{}]", net.getDisplayName(), String.join(",", net.getIPv4addr()));
        }

        // java version
        logger.info("Java: version = {}, xmx = {} MB", System.getProperty("java.version"),
                Runtime.getRuntime().maxMemory() / mb);
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
        if (state != State.RUNNING) {
            return;
        } else {
            state = State.STOPPING;
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

        // make sure no thread is reading/writing the state
        ReentrantReadWriteLock.WriteLock lock = stateLock.writeLock();
        lock.lock();
        for (DbName name : DbName.values()) {
            dbFactory.getDB(name).close();
        }
        lock.unlock();

        state = State.STOPPED;
    }

    /**
     * Returns the kernel state.
     *
     * @return
     */
    public State state() {
        return state;
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
    public Key getCoinbase() {
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
    public SemuxApiService getApi() {
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
