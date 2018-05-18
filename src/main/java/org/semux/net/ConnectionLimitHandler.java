/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class ConnectionLimitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionLimitHandler.class);

    private static final Cache<InetAddress, AtomicInteger> connectionCount = Caffeine.newBuilder()
            .maximumSize(16 * 1024).build();

    private final int maxInboundConnectionsPerIp;

    /**
     * The constructor of ConnectionLimitHandler.
     * 
     * @param maxConnectionsPerIp
     *            Maximum allowed connections of each unique IP address.
     */
    public ConnectionLimitHandler(int maxConnectionsPerIp) {
        this.maxInboundConnectionsPerIp = maxConnectionsPerIp;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
        if (connectionCount.get(address, k -> new AtomicInteger(0))
                .incrementAndGet() > maxInboundConnectionsPerIp) {
            logger.warn("Too many connections from {}", address.getHostAddress());
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
        AtomicInteger cnt = connectionCount.get(address, k -> new AtomicInteger(0));
        if (cnt.decrementAndGet() <= 0) {
            connectionCount.invalidate(address);
        }
    }

    /**
     * Get the connection count of an address
     *
     * @param address
     *            an IP address
     * @return current connection count
     */
    public static int getConnectionsCount(InetAddress address) {
        AtomicInteger cnt = connectionCount.getIfPresent(address);
        return cnt == null ? 0 : cnt.get();
    }

    /**
     * Check whether there is a counter of the provided address.
     *
     * @param address
     *            an IP address
     * @return whether there is a counter of the address.
     */
    public static boolean containsAddress(InetAddress address) {
        return connectionCount.getIfPresent(address) != null;
    }

    /**
     * Reset connection count
     */
    public static void reset() {
        connectionCount.invalidateAll();
    }
}
