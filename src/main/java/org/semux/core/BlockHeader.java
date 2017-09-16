/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.Arrays;

import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.utils.Bytes;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class BlockHeader {

    private byte[] hash;

    private long number;

    private byte[] coinbase;

    private byte[] prevHash;

    private long timestamp;

    private byte[] transactionsRoot;

    private byte[] resultsRoot;

    private byte[] stateRoot;

    private byte[] data;

    private byte[] encoded;
    private Signature signature;

    /**
     * Create an instance of block header.
     * 
     * @param number
     * @param coinbase
     * @param prevHash
     * @param timestamp
     * @param transactionsRoot
     * @param resultsRoot
     * @param data
     */
    public BlockHeader(long number, byte[] coinbase, byte[] prevHash, long timestamp, byte[] transactionsRoot,
            byte[] resultsRoot, byte[] stateRoot, byte[] data) {
        this.number = number;
        this.coinbase = coinbase;
        this.prevHash = prevHash;
        this.timestamp = timestamp;
        this.transactionsRoot = transactionsRoot;
        this.resultsRoot = resultsRoot;
        this.stateRoot = stateRoot;
        this.data = data;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(number);
        enc.writeBytes(coinbase);
        enc.writeBytes(prevHash);
        enc.writeLong(timestamp);
        enc.writeBytes(transactionsRoot);
        enc.writeBytes(resultsRoot);
        enc.writeBytes(stateRoot);
        enc.writeBytes(data);
        this.encoded = enc.toBytes();
        this.hash = Hash.h256(encoded);
    }

    /**
     * Parse block header from byte arrays.
     * 
     * @param hash
     * @param encoded
     * @param signature
     */
    public BlockHeader(byte[] hash, byte[] encoded, byte[] signature) {
        this.hash = hash;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.number = dec.readLong();
        this.coinbase = dec.readBytes();
        this.prevHash = dec.readBytes();
        this.timestamp = dec.readLong();
        this.transactionsRoot = dec.readBytes();
        this.resultsRoot = dec.readBytes();
        this.stateRoot = dec.readBytes();
        this.data = dec.readBytes();

        this.encoded = encoded;
        // allow null signature for genesis
        this.signature = signature.length == 0 ? null : Signature.fromBytes(signature);
    }

    /**
     * Sign this block header.
     * 
     * @param key
     * @return
     */
    public BlockHeader sign(EdDSA key) {
        this.signature = key.sign(this.hash);
        return this;
    }

    /**
     * Validate block header format and signature.
     *
     * @return true if valid, otherwise false
     */
    public boolean validate() {
        return hash != null && hash.length == 32 //
                && number >= 0 //
                && coinbase != null && coinbase.length == 20 //
                && prevHash != null && prevHash.length == 32 //
                && timestamp >= 0 //
                && transactionsRoot != null && transactionsRoot.length == 32 //
                && resultsRoot != null && resultsRoot.length == 32 //
                && stateRoot != null && stateRoot.length == 32 //
                && data != null && data.length < 512 //
                && encoded != null //
                && (number == 0 || signature != null) //
                && Arrays.equals(Hash.h256(encoded), hash) //
                && (number == 0 || EdDSA.verify(hash, signature));
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

    public byte[] getTransactionsRoot() {
        return transactionsRoot;
    }

    public byte[] getResultsRoot() {
        return resultsRoot;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public byte[] getData() {
        return data;
    }

    public Signature getSignature() {
        return signature;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(hash);
        enc.writeBytes(encoded);
        // allow null signature for genesis
        enc.writeBytes(signature == null ? Bytes.EMPY_BYTES : signature.toBytes());

        return enc.toBytes();
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] hash = dec.readBytes();
        byte[] encoded = dec.readBytes();
        byte[] signature = dec.readBytes();

        return new BlockHeader(hash, encoded, signature);
    }

    @Override
    public String toString() {
        return "BlockHeader [number=" + number + ", timestamp=" + timestamp + ", data=" + Hex.encode(data)
                + ", parentHash=" + Hex.encode(prevHash) + ", hash=" + Hex.encode(hash) + "]";
    }

}
