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
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.utils.MerkleUtil;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

/**
 * Represents a block in the blockchain.
 *
 */
public class Block implements Comparable<Block> {

    private static final ThreadFactory factory = new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "block-validator-" + cnt.getAndIncrement());
        }
    };

    /**
     * The block header.
     */
    private BlockHeader header;

    /**
     * The transactions.
     */
    private List<Transaction> transactions;

    /**
     * The transaction results.
     */
    private List<TransactionResult> results;

    /**
     * The BFT view and votes.
     */
    private int view;
    private List<Signature> votes;

    // =========================
    // Auxiliary data
    // =========================
    /**
     * Encoding of header and transactions.
     */
    protected byte[] encodedWithoutBFT;

    /**
     * Transaction indexes
     */
    protected List<Pair<Integer, Integer>> indexes = new ArrayList<>();

    /**
     * Indicate whether this is the Genesis block
     */
    protected boolean isGensis = false;

    /**
     * Create a new block, with no BFT information.
     * 
     * @param header
     *            a signed block header
     * @param transactions
     *            list of transactions
     * @param results
     *            list of transaction results
     */
    public Block(BlockHeader header, List<Transaction> transactions, List<TransactionResult> results) {
        this(header, transactions, results, 0, new ArrayList<>());
    }

    /**
     * Create a new block.
     * 
     * @param header
     *            a signed block header
     * @param transactions
     *            list of transaction
     * @param results
     *            list of transaction results
     * @param view
     *            BFT view
     * @param votes
     *            BFT validator votes
     */
    public Block(BlockHeader header, List<Transaction> transactions, List<TransactionResult> results, int view,
            List<Signature> votes) {
        this.header = header;

        this.transactions = transactions;
        this.results = results;

        this.view = view;
        this.votes = votes;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(header.toBytes());
        enc.writeInt(transactions.size());
        for (Transaction t : transactions) {
            int idx = enc.getWriteIndex() + 4 /* length code */;
            byte[] bytes = t.toBytes();
            enc.writeBytes(bytes);
            indexes.add(Pair.of(idx, idx + bytes.length));
        }
        enc.writeInt(results.size());
        for (TransactionResult r : results) {
            enc.writeBytes(r.toBytes());
        }
        this.encodedWithoutBFT = enc.toBytes();
    }

    /**
     * Validate block format and signature, and also validate the contained
     * transactions by calling {@link Transaction#validate()}.
     *
     * @param nThreads
     * @return true if valid, otherwise false
     */
    public boolean validate(int nThreads) {
        if (header != null && header.validate()) {
            // validate transactions
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
            } finally {
                exec.shutdownNow();
            }

            // validate transactions root
            byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(transactions);
            if (!Arrays.equals(transactionsRoot, header.getTransactionsRoot())) {
                return false;
            }

            // validate results root
            byte[] resultsRoot = MerkleUtil.computeResultsRoot(results);
            if (!Arrays.equals(resultsRoot, header.getResultsRoot())) {
                return false;
            }

            // validate state root
            return Arrays.equals(Hash.EMPTY_H256, header.getStateRoot());
        }

        return false;
    }

    /**
     * Validate block format and signature, along with transaction validation, using
     * half the available CPU cores.
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
        return header;
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
     * Get a shallow copy of the transactions results.
     * 
     * @return
     */
    public List<TransactionResult> getResults() {
        return new ArrayList<>(results);
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
        return new ArrayList<>(indexes);
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
        return header.getHash();
    }

    public long getNumber() {
        return header.getNumber();
    }

    public byte[] getCoinbase() {
        return header.getCoinbase();
    }

    public byte[] getPrevHash() {
        return header.getPrevHash();
    }

    public long getTimestamp() {
        return header.getTimestamp();
    }

    public byte[] getTransactionsRoot() {
        return header.getTransactionsRoot();
    }

    public byte[] getResultsRoot() {
        return header.getResultsRoot();
    }

    public byte[] getStateRoot() {
        return header.getStateRoot();
    }

    public byte[] getData() {
        return header.getData();
    }

    public Signature getSignature() {
        return header.getSignature();
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder(encodedWithoutBFT);
        enc.writeInt(view);
        enc.writeInt(votes.size());
        for (Signature vote : votes) {
            enc.writeBytes(vote.toBytes());
        }

        return enc.toBytes();
    }

    public static Block fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        BlockHeader header = BlockHeader.fromBytes(dec.readBytes());

        List<Transaction> transactions = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            transactions.add(Transaction.fromBytes(dec.readBytes()));
        }

        List<TransactionResult> results = new ArrayList<>();
        n = dec.readInt();
        for (int i = 0; i < n; i++) {
            results.add(TransactionResult.fromBytes(dec.readBytes()));
        }

        int view = dec.readInt();
        List<Signature> votes = new ArrayList<>();
        n = dec.readInt();
        for (int i = 0; i < n; i++) {
            votes.add(Signature.fromBytes(dec.readBytes()));
        }

        return new Block(header, transactions, results, view, votes);
    }

    @Override
    public int compareTo(Block other) {
        return Long.compare(this.getNumber(), other.getNumber());
    }

    @Override
    public String toString() {
        return "Block [number = " + getNumber() + ", view = " + getView() + ", hash = " + Hex.encode(getHash())
                + ", # txs = " + transactions.size() + ", # votes = " + votes.size() + "]";
    }
}
