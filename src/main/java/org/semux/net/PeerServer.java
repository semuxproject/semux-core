/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.semux.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Represents a server in the Semux network
 */
public class PeerServer {

    private static final Logger logger = LoggerFactory.getLogger(PeerServer.class);

    private static final ThreadFactory factory = new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "server-" + cnt.getAndIncrement());
        }
    };

    protected SemuxChannelInitializer channelHandler;

    protected boolean listening;

    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected ChannelFuture channelFuture;

    public PeerServer(SemuxChannelInitializer channelHandler) {
        this.channelHandler = channelHandler;
    }

    public void start(String ip, int port) {
        bossGroup = new NioEventLoopGroup(1, factory);
        workerGroup = new NioEventLoopGroup(0, factory);

        try {
            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);

            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Config.NET_TIMEOUT_CONNECT);

            b.handler(new LoggingHandler());
            b.childHandler(channelHandler);

            logger.info("Starting peer server: address = {}:{}", ip, port);
            channelFuture = b.bind(ip, port).sync();
            logger.debug("Binding was sucessfully");

            listening = true;
            channelFuture.channel().closeFuture().sync();
            logger.info("PeerServer shut down");

        } catch (Exception e) {
            logger.error("Failed to start peer server", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            listening = false;
        }
    }

    public void stop() {
        if (listening && channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                channelFuture.channel().close().sync();
            } catch (Exception e) {
                logger.error("Failed to close channel", e);
            }
        }
    }

    public boolean isListening() {
        return listening;
    }
}