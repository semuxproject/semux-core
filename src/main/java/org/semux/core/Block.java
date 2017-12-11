/**
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
import org.semux.crypto.Hex;
import org.semux.util.MerkleUtil;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

/**
 * Represents a block in the blockchain.
 *
 */
public class Block {

    private static final ThreadFactory factory = new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "block-" + cnt.getAndIncrement());
        }
    };

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService exec = Executors.newFixedThreadPool(CORES, factory);

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
     * Encoding of transactions.
     */
    protected byte[] encodedHeader;
    protected byte[] encodedTransactions;
    protected byte[] encodedResults;

    /**
     * Transaction indexes
     */
    protected List<Pair<Integer, Integer>> indexes = new ArrayList<>();

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

        this.encodedHeader = header.toBytes();
        SimpleEncoder encTx = new SimpleEncoder();
        SimpleEncoder encRe = new SimpleEncoder();
        encTx.writeInt(transactions.size());
        encRe.writeInt(results.size());
        for (int i = 0; i < transactions.size(); i++) {
            int idxTx = encTx.getWriteIndex();
            int idxRe = encRe.getWriteIndex();
            encTx.writeBytes(transactions.get(i).toBytes());
            encRe.writeBytes(results.get(i).toBytes());
            indexes.add(Pair.of(idxTx, idxRe));
        }
        this.encodedTransactions = encTx.toBytes();
        this.encodedResults = encRe.toBytes();
    }

    /**
     * Validates block header.
     * 
     * @param header
     * @param previous
     * @return
     */
    public static boolean validateHeader(BlockHeader previous, BlockHeader header) {
        return header != null && header.validate() //
                && header.getNumber() == previous.getNumber() + 1 //
                && Arrays.equals(header.getPrevHash(), previous.getHash()) //
                && header.getTimestamp() > previous.getTimestamp();
    }

    /**
     * Validates transactions in parallel.
     * 
     * @param header
     * @param transactions
     * @return
     */
    public static boolean validateTransactions(BlockHeader header, List<Transaction> transactions) {
        // validate transactions
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

        // validate transactions root
        byte[] root = MerkleUtil.computeTransactionsRoot(transactions);
        return Arrays.equals(root, header.getTransactionsRoot());
    }

    /**
     * Validates results.
     * 
     * @param header
     * @param results
     * @return
     */
    public static boolean validateResults(BlockHeader header, List<TransactionResult> results) {
        // validate results
        for (TransactionResult result : results) {
            if (!result.validate()) {
                return false;
            }
        }

        // validate results root
        byte[] root = MerkleUtil.computeResultsRoot(results);
        return Arrays.equals(root, header.getResultsRoot());
    }

    /**
     * Returns a shallow copy of the block header.
     * 
     * @return
     */
    public BlockHeader getHeader() {
        return header;
    }

    /**
     * Returns a shallow copy of the transactions.
     * 
     * @return
     */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    /**
     * Returns a shallow copy of the transactions results.
     * 
     * @return
     */
    public List<TransactionResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Returns the BFT view.
     * 
     * @return
     */
    public int getView() {
        return view;
    }

    /**
     * Sets the BFT view.
     * 
     * @param view
     */
    public void setView(int view) {
        this.view = view;
    }

    /**
     * Returns a shallow copy of the votes.
     * 
     * @return
     */
    public List<Signature> getVotes() {
        return new ArrayList<>(votes);
    }

    /**
     * Sets the votes for this block.
     * 
     * @param votes
     */
    public void setVotes(List<Signature> votes) {
        this.votes = votes;
    }

    /**
     * Returns a shallow copy of the transaction indexes;
     * 
     * @return
     */
    public List<Pair<Integer, Integer>> getTransactionIndices() {
        return new ArrayList<>(indexes);
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

    public byte[] toBytesHeader() {
        return encodedHeader;
    }

    public byte[] toBytesTransactions() {
        return encodedTransactions;
    }

    public byte[] toBytesResults() {
        return encodedResults;
    }

    public byte[] toBytesVotes() {
        SimpleEncoder enc = new SimpleEncoder();

        enc.writeInt(view);
        enc.writeInt(votes.size());
        for (Signature vote : votes) {
            enc.writeBytes(vote.toBytes());
        }

        return enc.toBytes();
    }

    /**
     * Get block size in bytes
     *
     * @return block size in bytes
     */
    public int size() {
        return toBytesHeader().length + toBytesTransactions().length + toBytesResults().length + toBytesVotes().length;
    }

    public static Block fromBytes(byte[] h, byte[] t, byte[] r, byte[] v) {
        BlockHeader header = BlockHeader.fromBytes(h);

        SimpleDecoder dec = new SimpleDecoder(t);
        List<Transaction> transactions = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            transactions.add(Transaction.fromBytes(dec.readBytes()));
        }

        dec = new SimpleDecoder(r);
        List<TransactionResult> results = new ArrayList<>();
        n = dec.readInt();
        for (int i = 0; i < n; i++) {
            results.add(TransactionResult.fromBytes(dec.readBytes()));
        }

        int view = 0;
        List<Signature> votes = new ArrayList<>();

        if (v != null) {
            dec = new SimpleDecoder(v);
            view = dec.readInt();
            n = dec.readInt();
            for (int i = 0; i < n; i++) {
                votes.add(Signature.fromBytes(dec.readBytes()));
            }
        }

        return new Block(header, transactions, results, view, votes);
    }

    public static Block fromBytes(byte[] h, byte[] t, byte[] r) {
        return fromBytes(h, t, r, null);
    }

    @Override
    public String toString() {
        return "Block [number = " + getNumber() + ", view = " + getView() + ", hash = " + Hex.encode(getHash())
                + ", # txs = " + transactions.size() + ", # votes = " + votes.size() + "]";
    }
}
