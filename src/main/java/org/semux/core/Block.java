/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.Network;
import org.semux.crypto.Hex;
import org.semux.crypto.Key.Signature;
import org.semux.util.MerkleUtil;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a block in the blockchain.
 */
public class Block {
    static final Logger logger = LoggerFactory.getLogger(Block.class);

    /**
     * The block header.
     */
    private final BlockHeader header;

    /**
     * The transactions.
     */
    private final List<Transaction> transactions;

    /**
     * The transaction results.
     */
    private final List<TransactionResult> results;

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
    protected final byte[] encodedHeader;
    protected final byte[] encodedTransactions;
    protected final byte[] encodedResults;

    /**
     * Transaction indexes
     */
    protected final List<Pair<Integer, Integer>> indexes = new ArrayList<>();

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
        if (header == null) {
            logger.warn("Header was null.");
            return false;
        }

        if (!header.validate()) {
            logger.warn("Header was invalid.");
            return false;
        }

        if (header.getNumber() != previous.getNumber() + 1) {
            logger.warn("Header number was not one greater than previous block.");
            return false;
        }

        if (!Arrays.equals(header.getParentHash(), previous.getHash())) {
            logger.warn("Header parent hash was not equal to previous block hash.");
            return false;
        }

        if (header.getTimestamp() <= previous.getTimestamp()) {
            logger.warn("Header timestamp was before previous block.");
            return false;
        }

        return true;
    }

    /**
     * Validates transactions in parallel.
     *
     * @param header
     * @param transactions
     * @param network
     * @return
     */
    public static boolean validateTransactions(BlockHeader header, List<Transaction> transactions, Network network) {
        return validateTransactions(header, transactions, transactions, network);
    }

    /**
     * Validates transactions in parallel, only doing those that have not already
     * been calculated.
     *
     * @param header
     *            block header
     * @param unvalidatedTransactions
     *            transactions needing validating
     * @param allTransactions
     *            all transactions within the block
     * @param network
     *            network
     * @return
     */
    public static boolean validateTransactions(BlockHeader header, Collection<Transaction> unvalidatedTransactions,
            List<Transaction> allTransactions, Network network) {
        // validate transactions
        boolean valid = unvalidatedTransactions.parallelStream().allMatch(tx -> tx.validate(network));
        if (!valid) {
            return false;
        }

        // validate transactions root
        byte[] root = MerkleUtil.computeTransactionsRoot(allTransactions);
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

    /**
     * Returns the block hash.
     *
     * @return
     */
    public byte[] getHash() {
        return header.getHash();
    }

    /**
     * Returns the block number.
     *
     * @return
     */
    public long getNumber() {
        return header.getNumber();
    }

    /**
     * Returns the coinbase
     *
     * @return
     */
    public byte[] getCoinbase() {
        return header.getCoinbase();
    }

    /**
     * Returns the hash of the parent block
     *
     * @return
     */
    public byte[] getParentHash() {
        return header.getParentHash();
    }

    /**
     * Returns the block timestamp.
     *
     * @return
     */
    public long getTimestamp() {
        return header.getTimestamp();
    }

    /**
     * Returns the merkle root of all transactions.
     *
     * @return
     */
    public byte[] getTransactionsRoot() {
        return header.getTransactionsRoot();
    }

    /**
     * Returns the merkle root of all transaction results.
     *
     * @return
     */
    public byte[] getResultsRoot() {
        return header.getResultsRoot();
    }

    /**
     * Returns the state root.
     *
     * @return
     */
    public byte[] getStateRoot() {
        return header.getStateRoot();
    }

    /**
     * Returns the extra data.
     *
     * @return
     */
    public byte[] getData() {
        return header.getData();
    }

    /**
     * Serializes the block header into byte array.
     *
     * @return
     */
    public byte[] getEncodedHeader() {
        return encodedHeader;
    }

    /**
     * Serializes the block transactions into byte array.
     *
     * @return
     */
    public byte[] getEncodedTransactions() {
        return encodedTransactions;
    }

    /**
     * Serializes the block transactions results into byte array.
     *
     * @return
     */
    public byte[] getEncodedResults() {
        return encodedResults;
    }

    /**
     * Serializes the BFT votes into byte array.
     *
     * @return
     */
    public byte[] getEncodedVotes() {
        SimpleEncoder enc = new SimpleEncoder();

        enc.writeInt(view);
        enc.writeInt(votes.size());
        for (Signature vote : votes) {
            enc.writeBytes(vote.toBytes());
        }

        return enc.toBytes();
    }

    /**
     * Parses a block instance from bytes.
     *
     * @param h
     *            Serialized header
     * @param t
     *            Serialized transactions
     * @param r
     *            Serialized transaction results
     * @param v
     *            Serialized votes
     * @return
     */
    public static Block fromComponents(byte[] h, byte[] t, byte[] r, byte[] v) {
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

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(getEncodedHeader());
        enc.writeBytes(getEncodedTransactions());
        enc.writeBytes(getEncodedResults());
        enc.writeBytes(getEncodedVotes());

        return enc.toBytes();
    }

    public static Block fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] header = dec.readBytes();
        byte[] transactions = dec.readBytes();
        byte[] results = dec.readBytes();
        byte[] votes = dec.readBytes();

        return Block.fromComponents(header, transactions, results, votes);
    }

    /**
     * Get block size in bytes
     *
     * @return block size in bytes
     */
    public int size() {
        return toBytes().length;
    }

    @Override
    public String toString() {
        return "Block [number = " + getNumber() + ", view = " + getView() + ", hash = " + Hex.encode(getHash())
                + ", # txs = " + transactions.size() + ", # votes = " + votes.size() + "]";
    }

}
