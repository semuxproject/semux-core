/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

/**
 * Represent a frame in the Semux network. Numbers are signed and in big-endian.
 * 
 * <ul>
 * <li>FRAME := HEADER (32 bytes, fixed) + BODY (variable length)</li>
 * <li>HEADER := SIZE + TYPE + PACKET_ID + PACKET_SIZE</li>
 * <li>BODY := RAW_DATA
 * </ul>
 */
public class Frame {
    public static final int HEADER_SIZE = 32;

    private int size; /* frame size, 4 bytes */
    private byte type; /* protocol type, 1 byte */
    private byte network; /* network id, 1 byte */

    // packets are split into multiple frames
    private int packetId; /* 4 bytes */
    private int packetSize; /* 4 bytes */

    private byte[] payload;

    public Frame(int size, byte type, byte network, int packetId, int packetSize, byte[] payload) {
        this.size = size;
        this.type = type;
        this.network = network;
        this.packetId = packetId;
        this.packetSize = packetSize;
        this.payload = payload;
    }

    public boolean isSingleFrame() {
        return size == packetSize;
    }

    public int getSize() {
        return size;
    }

    public byte getType() {
        return type;
    }

    public byte getNetwork() {
        return network;
    }

    public int getPacketId() {
        return packetId;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Frame [size=" + size + ", type=" + type + ", network=" + network + ", packetId=" + packetId
                + ", packetSize=" + packetSize + "]";
    }

}