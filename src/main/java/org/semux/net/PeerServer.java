/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.semux.Kernel;
import org.semux.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
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

    protected Kernel kernel;
    protected Channel channel;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public PeerServer(Kernel kernel) {
        this.kernel = kernel;
    }

    public void start() {
        start(kernel.getConfig().p2pListenIp(), kernel.getConfig().p2pListenPort());
    }

    public void start(String ip, int port) {
        if (isRunning()) {
            return;
        }

        try {
            bossGroup = new NioEventLoopGroup(1, factory);
            workerGroup = new NioEventLoopGroup(0, factory);

            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);

            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Constants.DEFAULT_CONNECT_TIMEOUT);

            b.handler(new LoggingHandler());
            b.childHandler(new SemuxChannelInitializer(kernel, null));

            logger.info("Starting peer server: address = {}:{}", ip, port);
            channel = b.bind(ip, port).sync().channel();
        } catch (Exception e) {
            logger.error("Failed to start peer server", e);
        }
    }

    public void stop() {
        if (isRunning() && channel.isOpen()) {
            try {
                channel.close().sync();

                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();

                // workerGroup.terminationFuture().sync();
                // bossGroup.terminationFuture().sync();

                channel = null;
            } catch (Exception e) {
                logger.error("Failed to close channel", e);
            }
            logger.info("PeerServer shut down");
        }
    }

    public boolean isRunning() {
        return channel != null;
    }
}