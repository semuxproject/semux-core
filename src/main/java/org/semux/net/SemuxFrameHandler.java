/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import java.io.IOException;
import java.util.List;

import org.semux.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

public class SemuxFrameHandler extends ByteToMessageCodec<Frame> {

    private static final Logger logger = LoggerFactory.getLogger(SemuxFrameHandler.class);

    private Config config;

    public SemuxFrameHandler(Config config) {
        this.config = config;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame frame, ByteBuf out) throws Exception {
        // NOTE: write() operation does not flush automatically

        ByteBuf buf = out.alloc().buffer(Frame.HEADER_SIZE + frame.getSize());

        int index = buf.writerIndex();
        buf.writeInt(frame.getSize());
        buf.writeByte(frame.getType());
        buf.writeByte(frame.getNetwork());
        buf.writeInt(frame.getPacketId());
        buf.writeInt(frame.getPacketSize());

        buf.writerIndex(index + Frame.HEADER_SIZE);
        buf.writeBytes(frame.getPayload());

        ctx.write(buf);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }

        // Read the size of this frame
        int index = in.readerIndex();
        int size = in.readInt();
        in.readerIndex(index);

        if (size < 0 || size > config.netMaxFrameSize()) {
            throw new IOException("Invalid frame size: " + size);
        }

        if (in.readableBytes() >= Frame.HEADER_SIZE + size) {
            size = in.readInt();
            byte type = in.readByte();
            byte network = in.readByte();
            int packetId = in.readInt();
            int packetSize = in.readInt();

            in.readerIndex(index + Frame.HEADER_SIZE);
            byte[] playload = new byte[size];
            in.readBytes(playload);

            Frame frame = new Frame(size, type, network, packetId, packetSize, playload);

            /*
             * If the peer is not in our network, drop connection immediately.
             */
            if (network != config.networkId()) {
                ctx.close();
                return;
            }

            if (size < 0 || type < 0 || packetId < 0 || packetSize < 0) {
                logger.debug("Invalid frame: {}", frame);
            } else {
                out.add(frame);
            }
        }
    }
}