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
import org.semux.net.msg.MessageException;
import org.semux.net.msg.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

public class SemuxMessageHandler extends MessageToMessageCodec<Frame, Message> {

    private static final Logger logger = LoggerFactory.getLogger(SemuxMessageHandler.class);

    private static final int MAX_PACKETS = 16;

    private static final byte COMPRESS_TYPE = Frame.COMPRESS_SNAPPY;

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
        byte[] dataCompressed = data;

        switch (COMPRESS_TYPE) {
        case Frame.COMPRESS_SNAPPY:
            dataCompressed = Snappy.compress(data);
            break;
        case Frame.COMPRESS_NONE:
            break;
        default:
            logger.error("Unsupported compress type: " + COMPRESS_TYPE);
            return;
        }

        byte packetType = msg.getCode().toByte();
        int packetId = count.incrementAndGet();
        int packetSize = dataCompressed.length;

        if (data.length > config.netMaxPacketSize() || dataCompressed.length > config.netMaxPacketSize()) {
            logger.error("Invalid packet size, max = {}, actual = {}", config.netMaxPacketSize(), packetSize);
            return;
        }

        int limit = config.netMaxFrameBodySize();
        int total = (dataCompressed.length - 1) / limit + 1;
        for (int i = 0; i < total; i++) {
            byte[] body = new byte[(i < total - 1) ? limit : dataCompressed.length % limit];
            System.arraycopy(dataCompressed, i * limit, body, 0, body.length);

            out.add(new Frame(Frame.VERSION, COMPRESS_TYPE, packetType, packetId, packetSize, body.length, body));
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
                    out.add(decodeMessage(pair.getLeft()));

                    // remove complete packets from cache
                    incompletePackets.invalidate(packetId);
                } else if (remaining < 0) {
                    throw new IOException("Packet remaining size went to negative");
                }
            }
        } else {
            out.add(decodeMessage(Collections.singletonList(frame)));
        }
    }

    /**
     * Decode message from the frames.
     * 
     * @param frames
     * @return
     * @throws MessageException
     */
    protected Message decodeMessage(List<Frame> frames) throws MessageException {
        if (frames == null || frames.isEmpty()) {
            throw new MessageException("Frames can't be null or empty");
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

        switch (head.getCompressType()) {
        case Frame.COMPRESS_SNAPPY:
            try {
                // check uncompressed length to avoid OOM vulnerability
                int length = Snappy.uncompressedLength(data);
                if (length > config.netMaxPacketSize()) {
                    throw new MessageException("Uncompressed data length is too big: " + length);
                }
                data = Snappy.uncompress(data);
            } catch (IOException e) {
                throw new MessageException(e);
            }
            break;
        case Frame.COMPRESS_NONE:
            break;
        default:
            throw new MessageException("Unsupported compress type: " + head.getCompressType());
        }

        return messageFactory.create(packetType, data);
    }
}