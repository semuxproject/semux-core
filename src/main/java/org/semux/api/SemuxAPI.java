/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.semux.Kernel;
import org.semux.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Semux API launcher
 *
 */
public class SemuxAPI {

    private static final Logger logger = LoggerFactory.getLogger(SemuxAPI.class);

    private static final ThreadFactory factory = new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "api-" + cnt.getAndIncrement());
        }
    };

    private Kernel kernel;
    private Config config;

    private ChannelFuture channelFuture;
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public SemuxAPI(Kernel kernel) {
        this.kernel = kernel;
        this.config = kernel.getConfig();
    }

    /**
     * Starts API server with configured binding address.
     */
    public void start() {
        start(config.apiListenIp(), config.apiListenPort(), new SemuxAPIHttpChannelInitializer());
    }

    /**
     * Starts API server at the given binding IP and port.
     *
     * @param ip
     * @param port
     */
    public void start(String ip, int port) {
        start(ip, port, new SemuxAPIHttpChannelInitializer());
    }

    /**
     * Starts API server at the given binding IP and port, with the specified
     * channel initializer.
     * 
     * @param ip
     * @param port
     * @param httpChannelInitializer
     */
    public void start(String ip, int port, HttpChannelInitializer httpChannelInitializer) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, factory);
        EventLoopGroup workerGroup = new NioEventLoopGroup(0, factory);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(httpChannelInitializer);

            logger.info("Starting API server: address = {}:{}", ip, port);
            channelFuture = b.bind(ip, port).sync();

            isRunning.set(true);
            channelFuture.channel().closeFuture().sync();
            logger.info("API server shut down");

        } catch (Exception e) {
            logger.error("Failed to start API server", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            isRunning.set(false);
        }
    }

    /**
     * Stops the API server if started.
     */
    public void stop() {
        if (isRunning() && channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                channelFuture.channel().close().sync();
            } catch (Exception e) {
                logger.error("Failed to close channel", e);
            }
        }
    }

    /**
     * Returns whether the API server is running or not.
     * 
     * @return
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * The default channel initializer using {@link ApiHandlerImpl}.
     */
    private class SemuxAPIHttpChannelInitializer extends HttpChannelInitializer {

        @Override
        public HttpHandler initHandler() {
            return new HttpHandler(config, new ApiHandlerImpl(kernel));
        }
    }
}
