/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import io.netty.buffer.ByteBuf;

/**
 * Represent a frame in the Semux network. Numbers are signed and in big-endian.
 * 
 * <ul>
 * <li><code>FRAME := HEADER (16 bytes) + BODY (variable length)</code></li>
 * <li><code>HEADER := VERSION + COMPRESS_TYPE + PACKET_TYPE + PACKET_ID + PACKET_SIZE + BODY_SIZE</code></li>
 * <li><code>BODY := BINARY_DATA</code></li>
 * </ul>
 */
public class Frame {

    public static final int HEADER_SIZE = 16;

    public static final short VERSION = 0;

    public static final byte COMPRESS_NONE = 0;
    public static final byte COMPRESS_SNAPPY = 1;

    protected final short version; /* version, 2 bytes */
    protected final byte compressType; /* compress type, 1 byte */
    protected final byte packetType; /* packet type, 1 byte */
    protected final int packetId; /* packet id, 4 bytes */
    protected final int packetSize; /* packet size, 4 bytes */
    protected final int bodySize; /* body size, 4 bytes */

    protected byte[] body;

    public Frame(short version, byte compressType, byte packetType, int packetId, int packetSize, int bodySize,
            byte[] body) {
        this.version = version;
        this.compressType = compressType;
        this.packetType = packetType;
        this.packetId = packetId;
        this.packetSize = packetSize;
        this.bodySize = bodySize;

        this.body = body;
    }

    public short getVersion() {
        return version;
    }

    public byte getCompressType() {
        return compressType;
    }

    public byte getPacketType() {
        return packetType;
    }

    public int getPacketId() {
        return packetId;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public int getBodySize() {
        return bodySize;
    }

    /**
     * Returns the frame body, which may be null.
     * 
     * @return
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Sets the frame body.
     * 
     * @param body
     */
    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
     * Returns whether the packet is chunked.
     * 
     * @return
     */
    public boolean isChunked() {
        return bodySize != packetSize;
    }

    /**
     * Writes frame header into the buffer.
     * 
     */
    public void writeHeader(ByteBuf buf) {
        buf.writeShort(getVersion());
        buf.writeByte(getCompressType());
        buf.writeByte(getPacketType());
        buf.writeInt(getPacketId());
        buf.writeInt(getPacketSize());
        buf.writeInt(getBodySize());
    }

    /**
     * Reads frame header from the given buffer.
     * 
     * @param in
     * @return
     */
    public static Frame readHeader(ByteBuf in) {
        short version = in.readShort();
        byte compressType = in.readByte();
        byte packetType = in.readByte();
        int packetId = in.readInt();
        int packetSize = in.readInt();
        int bodySize = in.readInt();

        return new Frame(version, compressType, packetType, packetId, packetSize, bodySize, null);
    }

    @Override
    public String toString() {
        return "Frame [version=" + version + ", compressType=" + compressType + ", packetType=" + packetType
                + ", packetId=" + packetId + ", packetSize=" + packetSize + ", bodySize=" + bodySize + "]";
    }
}