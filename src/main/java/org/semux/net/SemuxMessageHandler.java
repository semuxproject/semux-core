/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.config.Config;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

public class SemuxMessageHandler extends MessageToMessageCodec<Frame, Message> {

    private static final Logger logger = LoggerFactory.getLogger(SemuxMessageHandler.class);

    private static final int MAX_PACKETS = 16;

    private final Cache<Integer, Pair<List<Frame>, AtomicInteger>> incompletePackets = Caffeine.newBuilder()
            .maximumSize(MAX_PACKETS).build();

    private Config config;

    private MessageFactory messageFactory;
    private AtomicInteger count;

    public SemuxMessageHandler(Config config) {
        this.config = config;

        this.messageFactory = new MessageFactory();
        this.count = new AtomicInteger(0);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        byte[] data = msg.getEncoded();

        // TODO: compress data

        byte packetType = msg.getCode().toByte();
        int packetId = count.incrementAndGet();
        int packetSize = data.length;

        if (packetSize > config.netMaxPacketSize()) {
            logger.error("Invalid packet size, max = {}, actual = {}", config.netMaxPacketSize(), packetSize);
            return;
        }

        int limit = config.netMaxFrameBodySize();
        int total = (data.length - 1) / limit + 1;
        for (int i = 0; i < total; i++) {
            byte[] body = new byte[(i < total - 1) ? limit : data.length % limit];
            System.arraycopy(data, i * limit, body, 0, body.length);

            out.add(new Frame(Frame.VERSION, Frame.COMPRESS_NONE, packetType, packetId, packetSize, body.length, body));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Frame frame, List<Object> out) throws Exception {
        if (frame.isChunked()) {
            synchronized (incompletePackets) {
                int packetId = frame.getPacketId();
                Pair<List<Frame>, AtomicInteger> pair = incompletePackets.getIfPresent(packetId);
                if (pair == null) {
                    int packetSize = frame.getPacketSize();
                    if (packetSize < 0 || packetSize > config.netMaxPacketSize()) {
                        // this will kill the connection
                        throw new IOException("Invalid packet size: " + packetSize);
                    }

                    pair = Pair.of(new ArrayList<>(), new AtomicInteger(packetSize));
                    incompletePackets.put(packetId, pair);
                }

                pair.getLeft().add(frame);
                int remaining = pair.getRight().addAndGet(-frame.getBodySize());
                if (remaining == 0) {
                    Message msg = decodeMessage(pair.getLeft());

                    if (msg == null) {
                        throw new IOException("Failed to decode packet: pid = " + frame.getPacketId());
                    } else {
                        out.add(msg);
                    }

                    // remove complete packets from cache
                    incompletePackets.invalidate(packetId);

                } else if (remaining < 0) {
                    throw new IOException("Packet remaining size went to negative");
                }
            }
        } else {
            Message msg = decodeMessage(Collections.singletonList(frame));

            if (msg == null) {
                throw new IOException("Failed to decode packet: pid = " + frame.getPacketId());
            } else {
                out.add(msg);
            }
        }
    }

    /**
     * Decode message from the frames.
     * 
     * @param frames
     * @return
     */
    protected Message decodeMessage(List<Frame> frames) {
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        Frame head = frames.get(0);

        byte packetType = head.getPacketType();
        int packetSize = head.getPacketSize();

        byte[] data = new byte[packetSize];
        int pos = 0;
        for (Frame frame : frames) {
            System.arraycopy(frame.getBody(), 0, data, pos, frame.getBodySize());
            pos += frame.getBodySize();
        }

        // TODO: uncompress data

        return messageFactory.create(packetType, data);
    }
}