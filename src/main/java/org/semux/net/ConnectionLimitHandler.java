/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class ConnectionLimitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionLimitHandler.class);

    private static final ConcurrentHashMap<String, AtomicLong> connectionCount = new ConcurrentHashMap<>();

    private final long maxInboundConnectionsPerIp;

    /**
     * The
     * 
     * @param maxConnectionsPerIp
     */
    public ConnectionLimitHandler(long maxConnectionsPerIp) {
        this.maxInboundConnectionsPerIp = maxConnectionsPerIp;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        String address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        if (connectionCount.computeIfAbsent(address, k -> new AtomicLong(0))
                .incrementAndGet() > maxInboundConnectionsPerIp) {
            logger.warn("Too many connections from {}, closing the connection...", address);
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        String address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        connectionCount.computeIfPresent(address, (inetAddress, atomicLong) -> {
            atomicLong.decrementAndGet();
            return atomicLong;
        });
    }

    /**
     * Get the connection count of an address
     *
     * @param address
     *            an IP address
     * @return current connection count
     */
    public static long getConnectionsCount(String address) {
        return connectionCount.getOrDefault(address, new AtomicLong(0L)).get();
    }

    /**
     * Reset connection count
     */
    public static void reset() {
        connectionCount.clear();
    }
}
