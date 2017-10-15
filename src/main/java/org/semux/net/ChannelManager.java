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

import org.semux.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel Manager.
 * 
 */
public class ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    private Map<InetSocketAddress, Channel> channels = new HashMap<>();
    private Map<String, Channel> activeChannels = new HashMap<>();

    /**
     * Creates a channel manager instance.
     */
    public ChannelManager() {
    }

    /**
     * Returns whether an address is in the blacklist.
     * 
     * @param address
     * @return
     */
    public synchronized boolean isBlocked(InetSocketAddress address) {
        return Config.NET_BLACKLIST.contains(address.getAddress().getHostAddress());
    }

    /**
     * Returns whether a socket address is connected.
     * 
     * @param addr
     * @return
     */
    public synchronized boolean isConnected(InetSocketAddress addr) {
        return channels.containsKey(addr);
    }

    /**
     * Returns whether the specified IP is connected.
     * 
     * @param ip
     * @return
     */
    public synchronized boolean isConnectedIP(String ip) {
        for (InetSocketAddress addr : channels.keySet()) {
            if (addr.getAddress().getHostAddress().equals(ip)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether the specified peer is connected.
     * 
     * @param peerId
     * @return
     */
    public synchronized boolean isConnectedPeer(String peerId) {
        return activeChannels.containsKey(peerId);
    }

    /**
     * Returns the number of channels.
     * 
     * @return
     */
    public synchronized int size() {
        return channels.size();
    }

    /**
     * Adds a new channel to this manager.
     * 
     * @param ch
     *            channel instance
     */
    public synchronized void add(Channel ch) {
        logger.debug("Channel added: remoteAddress = {}:{}", ch.getRemoteIp(), ch.getRemotePort());

        channels.put(ch.getRemoteAddress(), ch);
    }

    /**
     * Removes a disconnected channel from this manager.
     * 
     * @param ch
     *            channel instance
     */
    public synchronized void remove(Channel ch) {
        logger.debug("Channel removed: remoteAddress = {}:{}", ch.getRemoteIp(), ch.getRemotePort());

        channels.remove(ch.getRemoteAddress());
        if (ch.isActive()) {
            activeChannels.remove(ch.getRemotePeer().getPeerId());
            ch.onDisconnect();
        }
    }

    /**
     * When a channel becomes active.
     * 
     * @param channel
     */
    public synchronized void onChannelActive(Channel channel) {
        activeChannels.put(channel.getRemotePeer().getPeerId(), channel);
    }

    /**
     * Returns a copy of the active peers.
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
     * Returns the listening IP addresses of active peers.
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
     * Returns the active channels.
     * 
     * @return
     */
    public synchronized List<Channel> getActiveChannels() {
        List<Channel> list = new ArrayList<>();
        list.addAll(activeChannels.values());

        return list;
    }

    /**
     * Returns the active channels, filtered by peerId.
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
     * Returns the active channels, whose message queue is idle.
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
