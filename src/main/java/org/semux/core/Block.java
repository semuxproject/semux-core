/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.utils.MerkleTree;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

/**
 * Represents a block in the blockchain.
 *
 */
public class Block implements Comparable<Block> {

    /**
     * The block header.
     */
    private byte[] hash;
    private long number;
    private byte[] coinbase;
    private byte[] prevHash;
    private long timestamp;
    private byte[] merkleRoot;
    private byte[] data;

    /**
     * A list of transactions.
     */
    private List<Transaction> transactions;

    private byte[] encoded;
    private Signature signature;

    /**
     * BFT-related info.
     */
    private int view = 0;
    private List<Signature> votes = new ArrayList<>();

    /* 4 + 32: block hash, 4 : encoded, 4: transaction size */
    protected int txIndexOffset = 36 + 4 + 4;
    protected List<Pair<Integer, Integer>> txIndex = new ArrayList<>();
    protected boolean isGensis = false;

    private static ThreadFactory factory = new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "block-validator-" + cnt.getAndIncrement());
        }
    };

    public Block(long number, byte[] coinbase, byte[] prevHash, long timestamp, byte[] merkleRoot, byte[] data,
            List<Transaction> transactions) {
        this.number = number;
        this.coinbase = coinbase;
        this.prevHash = prevHash;
        this.timestamp = timestamp;
        this.merkleRoot = merkleRoot;
        this.data = data;
        this.transactions = transactions;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(number);
        enc.writeBytes(coinbase);
        enc.writeBytes(prevHash);
        enc.writeLong(timestamp);
        enc.writeBytes(merkleRoot);
        enc.writeBytes(data);
        enc.writeInt(transactions.size());
        for (Transaction t : transactions) {
            int idx = enc.getWriteIndex() + txIndexOffset;

            byte[] bytes = t.toBytes();
            enc.writeBytes(bytes);
            txIndex.add(Pair.of(idx, idx + bytes.length));
        }
        this.encoded = enc.toBytes();
        this.hash = Hash.h256(encoded);
    }

    public Block(byte[] hash, byte[] encoded, byte[] signature) {
        this.hash = hash;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.number = dec.readLong();
        this.coinbase = dec.readBytes();
        this.prevHash = dec.readBytes();
        this.timestamp = dec.readLong();
        this.merkleRoot = dec.readBytes();
        this.data = dec.readBytes();

        transactions = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            int idx = dec.getReadIndex() + txIndexOffset;
            byte[] bytes = dec.readBytes();

            transactions.add(Transaction.fromBytes(bytes));
            txIndex.add(Pair.of(idx, idx + bytes.length));
        }

        this.encoded = encoded;
        this.signature = Signature.fromBytes(signature);
    }

    /**
     * Sign this transaction.
     * 
     * @param key
     * @return
     */
    public Block sign(EdDSA key) {
        this.signature = key.sign(getHash());
        return this;
    }

    /**
     * Validate block format and signature, along with transaction validation.
     * 
     * @param nThreads
     * @return
     */
    public boolean validate(int nThreads) {
        if (hash != null && hash.length == 32 //
                && number >= 0 //
                && coinbase != null && coinbase.length == 20 //
                && prevHash != null && prevHash.length == 32 //
                && timestamp >= 0 //
                && merkleRoot != null //
                && data != null && data.length < 1024 //
                && transactions != null && transactions.size() <= Config.MAX_BLOCK_SIZE //
                && encoded != null //
                && (number == 0 || signature != null) //

                && Arrays.equals(Hash.h256(encoded), hash) //
                && (number == 0 || EdDSA.verify(hash, signature))) {

            ExecutorService exec = Executors.newFixedThreadPool(nThreads, factory);
            try {
                List<Future<Boolean>> list = exec.invokeAll(transactions);
                for (Future<Boolean> f : list) {
                    if (!f.get()) {
                        return false;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
            exec.shutdownNow();

            List<byte[]> list = new ArrayList<>();
            for (Transaction tx : transactions) {
                list.add(tx.getHash());
            }
            return Arrays.equals(new MerkleTree(list).getRootHash(), merkleRoot);
        }

        return false;
    }

    /**
     * Validate block format and signature, along with transaction validation, using
     * half the available core.
     * 
     * @return
     */
    public boolean validate() {
        int cores = Runtime.getRuntime().availableProcessors();
        return validate(cores > 2 ? cores / 2 : 1);
    }

    /**
     * Get a shallow copy of the block header.
     * 
     * @return
     */
    public BlockHeader getHeader() {
        return new BlockHeader(hash, number, coinbase, prevHash, timestamp, merkleRoot, data);
    }

    /**
     * Get a shallow copy of the transactions.
     * 
     * @return
     */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    /**
     * Get the BFT view.
     * 
     * @return
     */
    public int getView() {
        return view;
    }

    /**
     * Set the BFT view.
     * 
     * @param view
     */
    public void setView(int view) {
        this.view = view;
    }

    /**
     * Get a shallow copy of the votes.
     * 
     * @return
     */
    public List<Signature> getVotes() {
        return new ArrayList<>(votes);
    }

    /**
     * Set the votes for this block.
     * 
     * @param votes
     */
    public void setVotes(List<Signature> votes) {
        this.votes = votes;
    }

    /**
     * Get a shallow copy of the transaction indexes;
     * 
     * @return
     */
    public List<Pair<Integer, Integer>> getTransacitonIndexes() {
        return new ArrayList<>(txIndex);
    }

    /**
     * Check if this is the genesis block.
     * 
     * @return
     */
    public boolean isGensis() {
        return isGensis;
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

    public Signature getSignature() {
        return signature;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(hash);
        enc.writeBytes(encoded);
        enc.writeBytes(signature.toBytes());

        enc.writeInt(view);
        enc.writeInt(votes.size());
        for (Signature vote : votes) {
            enc.writeBytes(vote.toBytes());
        }

        return enc.toBytes();
    }

    public static Block fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] hash = dec.readBytes();
        byte[] encoded = dec.readBytes();
        byte[] signature = dec.readBytes();

        int view = dec.readInt();
        int n = dec.readInt();
        List<Signature> votes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            votes.add(Signature.fromBytes(dec.readBytes()));
        }

        Block block = new Block(hash, encoded, signature);
        block.setView(view);
        block.setVotes(votes);

        return block;
    }

    @Override
    public int compareTo(Block other) {
        return Long.compare(this.getNumber(), other.getNumber());
    }

    @Override
    public String toString() {
        return "Block [number = " + getNumber() + ", view = " + getView() + ", hash = " + Hex.encode(hash)
                + ", # txs = " + transactions.size() + ", # votes = " + votes.size() + "]";
    }
}
