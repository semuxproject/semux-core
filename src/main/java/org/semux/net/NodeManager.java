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

import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class NodeManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);

    private static final ThreadFactory factory = new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "node-" + cnt.getAndIncrement());
        }
    };

    private static final String DNS_SEED_MAINNET = "mainnet.semux.org";
    private static final String DNS_SEED_TESTNET = "testnet.semux.org";

    private static final long MAX_QUEUE_SIZE = 1024;
    private static final int LRU_CACHE_SIZE = 1024;
    private static final long RECONNECT_WAIT = 2L * 60L * 1000L;

    private Kernel kernel;
    private Config config;

    private ChannelManager channelMgr;
    private PeerClient client;

    private Queue<Node> queue = new ConcurrentLinkedQueue<>();

    private Cache<Node, Long> lastConnect = Caffeine.newBuilder().maximumSize(LRU_CACHE_SIZE).build();

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> connectFuture;
    private ScheduledFuture<?> seedingFuture;

    private volatile boolean isRunning;

    /**
     * Creates a node manager instance.
     * 
     * @param kernel
     */
    public NodeManager(Kernel kernel) {
        this.kernel = kernel;
        this.config = kernel.getConfig();

        this.channelMgr = kernel.getChannelManager();
        this.client = kernel.getClient();

        this.exec = Executors.newSingleThreadScheduledExecutor(factory);
    }

    /**
     * Starts the node manager
     */
    public synchronized void start() {
        if (!isRunning) {
            addNodes(config.p2pSeedNodes());

            // every 0.5 seconds
            connectFuture = exec.scheduleAtFixedRate(this::doConnect, 100, 500, TimeUnit.MILLISECONDS);
            // every 120 seconds, delayed by 10 seconds (public IP lookup)
            seedingFuture = exec.scheduleAtFixedRate(this::doFetch, 10, 120, TimeUnit.SECONDS);

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
            seedingFuture.cancel(false);

            isRunning = false;
            logger.info("Node manager stopped");
        }
    }

    /**
     * Returns if the node manager is running or not.
     *
     * @return true if running, otherwise false
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Add a node to the connection queue.
     * 
     * @param node
     */
    public void addNode(Node node) {
        if (queueSize() < MAX_QUEUE_SIZE) {
            queue.add(node);
        }
    }

    /**
     * Add a collection of nodes to the connection queue.
     * 
     * @param nodes
     */
    public void addNodes(Collection<Node> nodes) {
        if (queueSize() < MAX_QUEUE_SIZE) {
            queue.addAll(nodes);
        }
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
     * @param networkId
     * @return
     */
    public static Set<Node> getSeedNodes(byte networkId) {
        Set<Node> nodes = new HashSet<>();

        String name = null;
        try {
            switch (networkId) {
            case Constants.MAIN_NET_ID:
                name = DNS_SEED_MAINNET;
                break;
            case Constants.TEST_NET_ID:
                name = DNS_SEED_TESTNET;
                break;
            default:
                return nodes;
            }

            for (InetAddress a : InetAddress.getAllByName(name)) {
                nodes.add(new Node(a, Constants.DEFAULT_P2P_PORT));
            }
        } catch (UnknownHostException e) {
            logger.info("Failed to get seed nodes from {}", name);
        }

        return nodes;
    }

    /**
     * Connect to a node in the queue.
     */
    protected void doConnect() {
        Set<InetSocketAddress> activeAddresses = channelMgr.getActiveAddresses();
        Node node;

        while ((node = queue.poll()) != null && channelMgr.size() < config.netMaxOutboundConnections()) {
            Long lastTouch = lastConnect.getIfPresent(node);
            long now = System.currentTimeMillis();

            if (!client.getNode().equals(node)
                    && !activeAddresses.contains(node.toAddress())
                    && (lastTouch == null || lastTouch + RECONNECT_WAIT < now)) {

                SemuxChannelInitializer ci = new SemuxChannelInitializer(kernel, node);
                client.connect(node, ci);
                lastConnect.put(node, now);
                break;
            }
        }
    }

    /**
     * Fetches seed nodes from DNS records or configuration.
     */
    protected void doFetch() {
        addNodes(getSeedNodes(config.networkId()));
    }

    /**
     * Represents a node in the semux network.
     */
    public static class Node {

        private InetSocketAddress address;

        /**
         * Construct a node with the given socket address.
         * 
         * @param address
         */
        public Node(InetSocketAddress address) {
            this.address = address;
        }

        /**
         * Construct a node with the given IP address and port.
         * 
         * @param ip
         * @param port
         */
        public Node(InetAddress ip, int port) {
            this(new InetSocketAddress(ip, port));
        }

        /**
         * Construct a node with the given IP address and port.
         * 
         * @param ip
         *            IP address, or hostname (not encouraged to use)
         * @param port
         *            port number
         */
        public Node(String ip, int port) {
            this(new InetSocketAddress(ip, port));
        }

        /**
         * Returns the IP address.
         * 
         * @return
         */
        public String getIp() {
            return address.getAddress().getHostAddress();
        }

        /**
         * Returns the port number
         * 
         * @return
         */
        public int getPort() {
            return address.getPort();
        }

        /**
         * Converts into a socket address.
         * 
         * @return
         */
        public InetSocketAddress toAddress() {
            return address;
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Node && address.equals(((Node) o).toAddress());
        }

        @Override
        public String toString() {
            return getIp() + ":" + getPort();
        }
    }
}
