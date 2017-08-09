/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetSocketAddress;

import org.semux.Config;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;

public class SemuxChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(SemuxChannelInitializer.class);

    private Blockchain chain;
    private PendingManager pendingMgr;
    private ChannelManager channelMgr;
    private NodeManager nodeMgr;

    private PeerClient client;
    private InetSocketAddress remoteAddress;

    private boolean isDiscoveryMode = false;

    /**
     * Create an instance of SemuxChannelInitializer.
     * 
     * 
     * @param chain
     *            the blockchain instance
     * @param pendingMgr
     *            the pending manager
     * @param channelMgr
     *            the channel manager
     * @param nodeMgr
     *            the node manager
     * @param client
     *            the peer client
     * @param remoteAddress
     *            the peer to connect, or null if in server mode
     */
    public SemuxChannelInitializer(Blockchain chain, PendingManager pendingMgr, ChannelManager channelMgr,
            NodeManager nodeMgr, PeerClient client, InetSocketAddress remoteAddress) {
        this.chain = chain;
        this.pendingMgr = pendingMgr;
        this.channelMgr = channelMgr;
        this.nodeMgr = nodeMgr;

        this.client = client;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void initChannel(NioSocketChannel ch) throws Exception {
        try {
            InetSocketAddress address = isInbound() ? ch.remoteAddress() : remoteAddress;
            logger.debug("New {} channel: remoteAddress = {}:{}", isInbound() ? "inbound" : "outbound",
                    address.getAddress().getHostAddress(), address.getPort());

            if (isInbound() && channelMgr.isBlocked(address)) {
                // avoid too frequent connection attempts
                logger.debug("Drop connection from a blocked peer, channel: {}", ch);
                ch.disconnect();
                return;
            }

            Channel channel = new Channel(chain, pendingMgr, channelMgr, nodeMgr);
            channel.init(ch.pipeline(), isInbound(), isDiscoveryMode, client, address);

            if (!isDiscoveryMode) {
                channelMgr.add(channel);
            }

            // limit the size of receiving buffer
            int bufferSize = Frame.HEADER_SIZE + Config.NET_MAX_FRAME_SIZE;
            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(bufferSize));
            ch.config().setOption(ChannelOption.SO_RCVBUF, bufferSize);
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

            // notify disconnection to channel manager
            ch.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!isDiscoveryMode) {
                        channelMgr.remove(channel);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Exception in channel initializer", e);
        }
    }

    private boolean isInbound() {
        return remoteAddress == null;
    }

    public void setDiscoveryMode(boolean isDiscoveryMode) {
        this.isDiscoveryMode = isDiscoveryMode;
    }
}