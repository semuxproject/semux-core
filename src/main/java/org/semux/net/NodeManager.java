/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.map.LRUMap;
import org.semux.Config;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);

    private static final ThreadFactory factory = new ThreadFactory() {

        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "node-mgr-" + cnt.getAndIncrement());
        }
    };

    private static String DNS_SEED_MAINNET = "mainnet.semux.org";
    private static String DNS_SEED_TESTNET = "testnet.semux.org";

    private static int DEFAULT_PORT = 5161;

    private static int LRU_CACHE_SIZE = 1024;
    private static long RECONNECT_WAIT = 2 * 60 * 1000;

    private Blockchain chain;
    private ChannelManager channelMgr;
    private PendingManager pendingMgr;
    private PeerClient client;

    private Queue<InetSocketAddress> queue;
    private LRUMap<InetSocketAddress, Long> lastConnect; // not thread-safe

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> connectFuture;
    private ScheduledFuture<?> persistFuture;

    private volatile boolean isRunning;

    /**
     * Creates a node manager instance.
     * 
     * @param chain
     * @param channelMgr
     * @param pendingMgr
     * @param client
     */
    public NodeManager(Blockchain chain, ChannelManager channelMgr, PendingManager pendingMgr, PeerClient client) {
        this.chain = chain;
        this.pendingMgr = pendingMgr;
        this.channelMgr = channelMgr;
        this.client = client;

        this.queue = new ConcurrentLinkedQueue<>();
        this.lastConnect = new LRUMap<>(LRU_CACHE_SIZE);

        this.exec = Executors.newSingleThreadScheduledExecutor(factory);
    }

    /**
     * Starts the node manager
     */
    public synchronized void start() {
        if (!isRunning) {
            /*
             * Push all known peers to the queue.
             */
            Set<InetSocketAddress> peers = new HashSet<>();
            peers.addAll(Config.P2P_SEED_NODES);
            peers.addAll(getSeedNodes(Config.NETWORK_ID));
            queue.addAll(peers);

            connectFuture = exec.scheduleAtFixedRate(this::doConnect, 100, 500, TimeUnit.MILLISECONDS);

            persistFuture = exec.scheduleAtFixedRate(this::doFetch, 2, 4, TimeUnit.MINUTES);

            isRunning = true;
            logger.info("Node manager started");
        }
    }

    /**
     * Stops this node manager.
     */
    public synchronized void stop() {
        if (isRunning) {
            connectFuture.cancel(true);
            persistFuture.cancel(false);

            isRunning = false;
            logger.info("Node manager stopped");
        }
    }

    /**
     * Add a node to the connection queue.
     * 
     * @param node
     */
    public void addNode(InetSocketAddress node) {
        queue.add(node);
    }

    /**
     * Add a collection of nodes to the connection queue.
     * 
     * @param nodes
     */
    public void addNodes(Collection<InetSocketAddress> nodes) {
        queue.addAll(nodes);
    }

    /**
     * Get the connection queue size.
     * 
     * @return
     */
    public int queueSize() {
        return queue.size();
    }

    /**
     * Get seed nodes from DNS records.
     * 
     * @param network
     * @return
     */
    public static Set<InetSocketAddress> getSeedNodes(short network) {
        Set<InetSocketAddress> nodes = new HashSet<>();

        try {
            String name;
            if (network == 0) {
                name = DNS_SEED_MAINNET;
            } else if (network == 1) {
                name = DNS_SEED_TESTNET;
            } else {
                return nodes;
            }

            for (InetAddress addr : InetAddress.getAllByName(name)) {
                nodes.add(new InetSocketAddress(addr, DEFAULT_PORT));
            }
        } catch (UnknownHostException e) {
            logger.info("Failed to get bootstrapping nodes by dns");
        }

        return nodes;
    }

    /**
     * Connect to a node
     */
    protected void doConnect() {
        Set<InetSocketAddress> activeAddresses = channelMgr.getActiveAddresses();
        InetSocketAddress addr;

        while ((addr = queue.poll()) != null && channelMgr.size() < Config.NET_MAX_CONNECTIONS) {
            Long l = lastConnect.get(addr);
            long now = System.currentTimeMillis();

            if (!new InetSocketAddress(client.getIp(), client.getPort()).equals(addr)//
                    && !activeAddresses.contains(addr) //
                    && (l == null || l + RECONNECT_WAIT < now)) {

                SemuxChannelInitializer ci = new SemuxChannelInitializer(chain, channelMgr, pendingMgr, this, client,
                        addr);
                client.connectAsync(addr, ci);
                lastConnect.put(addr, now);
                break;
            }
        }
    }

    /**
     * Fetch seed nodes from DNS records.
     */
    protected void doFetch() {
        addNodes(getSeedNodes(Config.NETWORK_ID));
    }
}
