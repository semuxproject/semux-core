/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public abstract class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec(HttpConstants.MAX_INITIAL_LINE_LENGTH, HttpConstants.MAX_HEADER_SIZE,
                HttpConstants.MAX_CHUNK_SIZE));
        p.addLast(new HttpServerKeepAliveHandler());
        p.addLast(new HttpObjectAggregator(HttpConstants.MAX_BODY_SIZE));
        p.addLast(new ChunkedWriteHandler());
        p.addLast(initHandler());
    }

    public abstract HttpHandler initHandler();
}
