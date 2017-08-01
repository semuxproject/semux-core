/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.crypto.Hex;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class BlockHeader {

    private byte[] hash;

    private long number;

    private byte[] coinbase;

    private byte[] prevHash;

    private long timestamp;

    private byte[] merkleRoot;

    private byte[] data;

    /**
     * Create an instance of block header.
     * 
     * @param hash
     * @param number
     * @param coinbase
     * @param prevHash
     * @param timestamp
     * @param merkleRoot
     * @param data
     */
    public BlockHeader(byte[] hash, long number, byte[] coinbase, byte[] prevHash, long timestamp, byte[] merkleRoot,
            byte[] data) {
        this.hash = hash;
        this.number = number;
        this.coinbase = coinbase;
        this.prevHash = prevHash;
        this.timestamp = timestamp;
        this.merkleRoot = merkleRoot;
        this.data = data;
    }

    public byte[] getHash() {
        return hash;
    }

    public long getNumber() {
        return number;
    }

    public byte[] getCoinbase() {
        return coinbase;
    }

    public byte[] getPrevHash() {
        return prevHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "BlockHeader [number=" + number + ", timestamp=" + timestamp + ", data=" + Hex.encode(data)
                + ", parentHash=" + Hex.encode(prevHash) + ", hash=" + Hex.encode(hash) + "]";
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(hash);
        enc.writeLong(number);
        enc.writeBytes(coinbase);
        enc.writeBytes(prevHash);
        enc.writeLong(timestamp);
        enc.writeBytes(merkleRoot);
        enc.writeBytes(data);

        return enc.toBytes();
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] hash = dec.readBytes();
        long number = dec.readLong();
        byte[] coinbase = dec.readBytes();
        byte[] parentHash = dec.readBytes();
        long timestamp = dec.readLong();
        byte[] merkleRoot = dec.readBytes();
        byte[] data = dec.readBytes();

        return new BlockHeader(hash, number, coinbase, parentHash, timestamp, merkleRoot, data);
    }
}
