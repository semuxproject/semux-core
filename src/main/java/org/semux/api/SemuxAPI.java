/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
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
            return new Thread(r, "api-server-" + cnt.getAndIncrement());
        }
    };

    private APIHandler handler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    private boolean listening;

    public SemuxAPI(APIHandler handler) {
        this.handler = handler;
    }

    public void start(String ip, int port) {
        bossGroup = new NioEventLoopGroup(1, factory);
        workerGroup = new NioEventLoopGroup(0, factory);
        try {
            ServerBootstrap b = new ServerBootstrap();
            ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new HttpRequestDecoder());
                    p.addLast(new HttpResponseEncoder());
                    p.addLast(new SemuxHttpHandler(handler));
                }
            };
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(initializer);

            logger.info("Starting API server: [{}:{}]", ip, port);
            channelFuture = b.bind(ip, port).sync();

            listening = true;
            channelFuture.channel().closeFuture().sync();
            logger.info("API server shut down");

        } catch (Exception e) {
            logger.error("Failed to start API server", e);
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
