/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.semux.Kernel;
import org.semux.config.Constants;
import org.semux.net.filter.SemuxIpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel Manager.
 * 
 * TODO: investigate handshake re-initialization.
 */
public class ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    /**
     * All channels, indexed by the <code>remoteAddress (ip + port)</code>, not
     * necessarily the listening address.
     */
    protected ConcurrentHashMap<InetSocketAddress, Channel> channels = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, Channel> activeChannels = new ConcurrentHashMap<>();

    protected final SemuxIpFilter ipFilter;

    public ChannelManager(Kernel kernel) {
        Path path = Paths.get(kernel.getConfig().dataDir().getAbsolutePath(), Constants.CONFIG_DIR, "ipfilter.json");

        ipFilter = (new SemuxIpFilter.Loader()).load(path).orElse(null);
    }

    /**
     * Returns the IP filter if enabled.
     * 
     * @return
     */
    public SemuxIpFilter getIpFilter() {
        return ipFilter;
    }

    /**
     * Returns whether a connection from the given address is acceptable or not.
     * 
     * @param address
     * @return
     */
    public boolean isAcceptable(InetSocketAddress address) {
        return ipFilter == null || ipFilter.isAcceptable(address);
    }

    /**
     * Returns whether a socket address is connected.
     * 
     * @param address
     * @return
     */
    public boolean isConnected(InetSocketAddress address) {
        return channels.containsKey(address);
    }

    /**
     * Returns whether the specified IP is connected.
     * 
     * @param ip
     * @return
     */
    public boolean isActiveIP(String ip) {
        for (Channel c : activeChannels.values()) {
            if (c.getRemoteIp().equals(ip)) {
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
    public boolean isActivePeer(String peerId) {
        return activeChannels.containsKey(peerId);
    }

    /**
     * Returns the number of channels.
     * 
     * @return
     */
    public int size() {
        return channels.size();
    }

    /**
     * Adds a new channel to this manager.
     * 
     * @param ch
     *            channel instance
     */
    public void add(Channel ch) {
        logger.debug("Channel added: remoteAddress = {}:{}", ch.getRemoteIp(), ch.getRemotePort());

        channels.put(ch.getRemoteAddress(), ch);
    }

    /**
     * Removes a disconnected channel from this manager.
     * 
     * @param ch
     *            channel instance
     */
    public void remove(Channel ch) {
        logger.debug("Channel removed: remoteAddress = {}:{}", ch.getRemoteIp(), ch.getRemotePort());

        channels.remove(ch.getRemoteAddress());
        if (ch.isActive()) {
            activeChannels.remove(ch.getRemotePeer().getPeerId());
            ch.onInactive();
        }
    }

    /**
     * When a channel becomes active.
     * 
     * @param channel
     * @param peer
     */
    public void onChannelActive(Channel channel, Peer peer) {
        channel.onActive(peer);
        activeChannels.put(peer.getPeerId(), channel);
    }

    /**
     * Returns a copy of the active peers.
     * 
     * @return
     */
    public List<Peer> getActivePeers() {
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
    public Set<InetSocketAddress> getActiveAddresses() {
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
    public List<Channel> getActiveChannels() {
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
    public List<Channel> getActiveChannels(List<String> peerIds) {
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
    public List<Channel> getIdleChannels() {
        List<Channel> list = new ArrayList<>();

        for (Channel c : activeChannels.values()) {
            if (c.getMessageQueue().isIdle()) {
                list.add(c);
            }
        }

        return list;
    }
}