/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.LRUMap;
import org.semux.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel Manager.
 * 
 */
public class ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    private static final long BLACK_LIST_EXPIRE = 1 * 60 * 1000; // 1 minutes

    private Map<InetSocketAddress, Channel> channels = new HashMap<>();
    private Map<String, Channel> activeChannels = new HashMap<>();

    private LRUMap<String, Long> blacklist = new LRUMap<>(5 * 1024);

    /**
     * Create a new channel manager.
     */
    public ChannelManager() {
    }

    /**
     * Check if an address is in the blacklist.
     * 
     * @param address
     * @return
     */
    public synchronized boolean isBlocked(InetSocketAddress address) {
        if (Config.NETWORK_ID != 0) {
            return false;
        }

        String k = address.getAddress().getHostAddress();
        Long v = blacklist.get(k);

        return v != null && v + BLACK_LIST_EXPIRE < System.currentTimeMillis();
    }

    /**
     * Check if a Socket address is connected.
     * 
     * @param addr
     * @return
     */
    public synchronized boolean isConnected(InetSocketAddress addr) {
        return channels.containsKey(addr);
    }

    /**
     * Check if the specified peer is connected.
     * 
     * @param peerId
     * @return
     */
    public synchronized boolean isConnected(String peerId) {
        return activeChannels.containsKey(peerId);
    }

    /**
     * Get the number of channels.
     * 
     * @return
     */
    public synchronized int size() {
        return channels.size();
    }

    /**
     * Notify this manager of a new channel.
     * 
     * @param ch
     *            channel instance
     */
    public synchronized void add(Channel ch) {
        logger.debug("Channel added: remoteAddress = {}:{}", ch.getRemoteIp(), ch.getRemotePort());

        channels.put(ch.getRemoteAddress(), ch);

        // TODO: is one connection per IP reasonable?
        blacklist.put(ch.getRemoteIp(), System.currentTimeMillis());
    }

    /**
     * Notify this manager of a disconnected channel.
     * 
     * @param ch
     *            channel instance
     */
    public synchronized void remove(Channel ch) {
        logger.debug("Channel removed: remoteAddress = {}:{}", ch.getRemoteIp(), ch.getRemotePort());

        channels.remove(ch.getRemoteAddress());
        if (ch.isActive()) {
            activeChannels.remove(ch.getRemotePeer().getPeerId());
            ch.setRemotePeer(null);
        }
    }

    /**
     * Notify this manager of an active channel.
     * 
     * @param channel
     */
    public synchronized void active(Channel channel) {
        activeChannels.put(channel.getRemotePeer().getPeerId(), channel);
    }

    /**
     * Get the active peers.
     * 
     * @return
     */
    public synchronized List<Peer> getActivePeers() {
        List<Peer> list = new ArrayList<>();
        for (Channel c : activeChannels.values()) {
            list.add(c.getRemotePeer());
        }

        return list;
    }

    /**
     * Get the listening IP addresses of active peers.
     * 
     * @return
     */
    public synchronized Set<InetSocketAddress> getActiveAddresses() {
        Set<InetSocketAddress> set = new HashSet<>();

        for (Channel c : activeChannels.values()) {
            Peer p = c.getRemotePeer();
            set.add(new InetSocketAddress(p.getIp(), p.getPort()));
        }

        return set;
    }

    /**
     * Get the active channels.
     * 
     * @return
     */
    public synchronized List<Channel> getActiveChannels() {
        List<Channel> list = new ArrayList<>();
        list.addAll(activeChannels.values());

        return list;
    }

    /**
     * Get the active channels, with the given filter.
     * 
     * @param peerIds
     *            peerId filter
     * @return
     */
    public synchronized List<Channel> getActiveChannels(List<String> peerIds) {
        List<Channel> list = new ArrayList<>();
        for (String peerId : peerIds) {
            if (activeChannels.containsKey(peerId)) {
                list.add(activeChannels.get(peerId));
            }
        }

        return list;
    }

    /**
     * Get the active channels, whose message queue is idle.
     * 
     * @return
     */
    public synchronized List<Channel> getIdleChannels() {
        List<Channel> list = new ArrayList<>();
        for (Channel c : activeChannels.values()) {
            if (c.getMessageQueue().isIdle()) {
                list.add(c);
            }
        }

        return list;
    }
}
