/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.semux.utils.IOUtil;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class NodeManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);

    private static String SEED_DNS_HOST = "seed-%d.semux.org";

    private static String PEERS_DIR = "p2p";
    private static String PEERS_FILE = "peers-%d.data";

    private static int LRU_CACHE_SIZE = 1024;
    private static long RECONNECT_WAIT = 2 * 60 * 1000;

    private Blockchain chain;
    private ChannelManager channelMgr;
    private PeerClient client;

    private Queue<InetSocketAddress> queue;
    private LRUMap<InetSocketAddress, Long> lastConnect; // not thread-safe

    private ScheduledExecutorService exec;
    private ScheduledFuture<?> connectFuture;
    private ScheduledFuture<?> persistFuture;

    private boolean isRunning;

    private static ThreadFactory factory = new ThreadFactory() {

        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "node-mgr-" + cnt.getAndIncrement());
        }
    };

    /**
     * Create a node manager instance.
     * 
     * @param chain
     * @param channelMgr
     * @param client
     */
    public NodeManager(Blockchain chain, ChannelManager channelMgr, PeerClient client) {
        this.chain = chain;
        this.channelMgr = channelMgr;
        this.client = client;

        this.queue = new ConcurrentLinkedQueue<>();
        this.lastConnect = new LRUMap<>(LRU_CACHE_SIZE);

        this.exec = Executors.newSingleThreadScheduledExecutor(factory);
    }

    /**
     * Start the node manager
     */
    public synchronized void start() {
        if (!isRunning) {
            /*
             * Push all known peers to the queue.
             */
            Set<InetSocketAddress> peers = new HashSet<>();
            peers.addAll(Config.P2P_SEED_NODES);
            peers.addAll(getSeedNodes(Config.NETWORK));
            peers.addAll(getPersistedNodes(Config.NETWORK));
            queue.addAll(peers);

            connectFuture = exec.scheduleAtFixedRate(() -> {
                doConnect();
            }, 100, 500, TimeUnit.MILLISECONDS);

            persistFuture = exec.scheduleAtFixedRate(() -> {
                doPersist();
            }, 2, 4, TimeUnit.MINUTES);

            isRunning = true;
            logger.info("Node manager started");
        }
    }

    /**
     * Stop this node manager.
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
     * @param networkId
     * @return
     */
    public static Set<InetSocketAddress> getSeedNodes(short networkId) {
        Set<InetSocketAddress> nodes = new HashSet<>();

        try {
            String domain = String.format(SEED_DNS_HOST, networkId);
            Lookup lookup = new Lookup(domain, Type.TXT);
            lookup.setResolver(new SimpleResolver());
            lookup.setCache(null);
            Record[] records = lookup.run();

            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                for (Record record : records) {
                    TXTRecord txt = (TXTRecord) record;
                    for (Object str : txt.getStrings()) {
                        String[] urls = str.toString().split(",");
                        for (String url : urls) {
                            String[] tokens = url.trim().split(":");
                            nodes.add(new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])));
                        }
                    }
                }
            }
        } catch (TextParseException | UnknownHostException e) {
            logger.debug("Failed to get bootstrapping nodes by dns");
        }

        return nodes;
    }

    /**
     * Get persisted nodes from disk.
     * 
     * @param network
     * @return
     */
    public static Set<InetSocketAddress> getPersistedNodes(byte network) {
        Set<InetSocketAddress> nodes = new HashSet<>();

        File f = getFile(network);
        if (f.exists()) {
            try {
                byte[] bytes = IOUtil.readFile(f);
                SimpleDecoder dec = new SimpleDecoder(bytes);
                int n = dec.readInt();
                for (int i = 0; i < n; i++) {
                    String host = dec.readString();
                    int port = dec.readInt();
                    nodes.add(new InetSocketAddress(host, port));
                }
            } catch (IOException e) {
                logger.debug("Failed to parse peer list from {}" + f, e);
            }
        }

        return nodes;
    }

    /**
     * Write peer list to disk.
     * 
     * @param network
     * @param nodes
     * @return
     */
    public static boolean setPersistedNodes(byte network, Collection<InetSocketAddress> nodes) {
        File f = getFile(network);
        try {
            SimpleEncoder enc = new SimpleEncoder();
            enc.writeInt(nodes.size());
            for (InetSocketAddress node : nodes) {
                enc.writeString(node.getAddress().getHostAddress());
                enc.writeInt(node.getPort());
            }

            IOUtil.writeToFile(enc.toBytes(), f);
            return true;
        } catch (IOException e) {
            logger.debug("Failed to write peer list into disk", e);
        }

        return false;
    }

    /**
     * Get the file that stores node list.
     * 
     * @param networkId
     * @return
     */
    private static File getFile(short networkId) {
        File f = new File(Config.DATA_DIR, PEERS_DIR + File.separator + String.format(PEERS_FILE, networkId));
        f.getParentFile().mkdirs();

        return f;
    }

    private void doConnect() {
        Set<InetSocketAddress> activeAddresses = channelMgr.getActiveAddresses();
        InetSocketAddress addr;

        while ((addr = queue.poll()) != null && channelMgr.size() < Config.NET_MAX_CONNECTIONS) {
            Long l = lastConnect.get(addr);
            long now = System.currentTimeMillis();

            if (!new InetSocketAddress(client.getIp(), client.getPort()).equals(addr)//
                    && !activeAddresses.contains(addr) //
                    && (l == null || l + RECONNECT_WAIT < now)) {

                SemuxChannelInitializer ci = new SemuxChannelInitializer(chain, channelMgr, this, client, addr);
                client.connectAsync(addr, ci);
                lastConnect.put(addr, now);
                break;
            }
        }
    }

    private void doPersist() {
        List<InetSocketAddress> list = new ArrayList<>(channelMgr.getActiveAddresses());
        setPersistedNodes(Config.NETWORK, list);

        addNodes(getSeedNodes(Config.NETWORK));
    }
}
