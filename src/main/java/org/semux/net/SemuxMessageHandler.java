/*
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.Config;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

public class SemuxMessageHandler extends MessageToMessageCodec<Frame, Message> {

    private static final Logger logger = LoggerFactory.getLogger(SemuxMessageHandler.class);

    protected Map<Integer, Pair<List<Frame>, AtomicInteger>> incompletePackets = Collections
            .synchronizedMap(new LRUMap<>(Config.NET_MAX_PACKET_SIZE));

    @SuppressWarnings("unused")
    private Channel channel;

    private MessageFactory messageFactory;

    private AtomicInteger count;

    public SemuxMessageHandler(Channel channel) {
        this.channel = channel;
        this.messageFactory = new MessageFactory();
        this.count = new AtomicInteger(0);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        byte[] encoded = msg.getEncoded();
        int packetId = count.incrementAndGet() % Integer.MAX_VALUE;
        int packetSize = encoded.length;
        byte type = msg.getCode().toByte();
        byte network = Config.NETWORK_ID;

        int limit = Config.NET_MAX_FRAME_SIZE;
        int total = (encoded.length - 1) / limit + 1;
        for (int i = 0; i < total; i++) {
            byte[] playload = new byte[(i < total - 1) ? limit : encoded.length % limit];
            System.arraycopy(encoded, i * limit, playload, 0, playload.length);

            Frame f = new Frame(playload.length, type, network, packetId, packetSize, playload);
            out.add(f);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Frame frame, List<Object> out) throws Exception {
        if (frame.isSingleFrame()) {
            Message msg = decodeMessage(ctx, Collections.singletonList(frame));

            if (msg == null) {
                logger.debug("Failed to decode packet into message: {}", frame);
            } else {
                out.add(msg);
            }
        } else {
            int packetId = frame.getPacketId();
            Pair<List<Frame>, AtomicInteger> pair = incompletePackets.get(packetId);
            if (pair == null) {
                pair = Pair.of(new ArrayList<>(), new AtomicInteger(frame.getPacketSize()));
                incompletePackets.put(packetId, pair);
            }

            pair.getLeft().add(frame);
            int remaining = pair.getRight().addAndGet(-frame.getSize());
            if (remaining == 0) {
                Message msg = decodeMessage(ctx, pair.getLeft());

                if (msg == null) {
                    logger.debug("Failed to decode packets into message, 1st/{}: {}", pair.getLeft().size(), frame);
                } else {
                    out.add(msg);
                }
            } else if (remaining < 0) {
                logger.debug("Corrupted packet, packetId: {}", packetId);
                incompletePackets.remove(packetId);
            }
        }
    }

    private Message decodeMessage(ChannelHandlerContext ctx, List<Frame> frames) throws IOException {
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        Frame head = frames.get(0);

        byte type = head.getType();
        int packetSize = head.getPacketSize();

        byte[] buffer = new byte[packetSize];
        int pos = 0;
        for (Frame frame : frames) {
            System.arraycopy(frame.getPayload(), 0, buffer, pos, frame.getSize());
            pos += frame.getSize();
        }

        return messageFactory.create(type, buffer);
    }
}