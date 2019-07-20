/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.semux.core.Amount.ZERO;
import static org.semux.core.Amount.sum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
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
    }

    public Block(BlockHeader header, List<Transaction> transactions) {
        this(header, transactions, new ArrayList<>(), 0, new ArrayList<>());
    }

    public Block(BlockHeader header, List<Transaction> transactions, List<TransactionResult> results) {
        this(header, transactions, results, 0, new ArrayList<>());
    }

    public void setResults(List<TransactionResult> results) {
        this.results = results;
    }

    public void setView(int view) {
        this.view = view;
    }

    public void setVotes(List<Signature> votes) {
        this.votes = votes;
    }

    /**
     * Validates block header.
     *
     * @param header
     * @param parentHeader
     * @return
     */
    public boolean validateHeader(BlockHeader header, BlockHeader parentHeader) {
        if (header == null) {
            logger.warn("Header was null.");
            return false;
        }

        if (!header.validate()) {
            logger.warn("Header was invalid.");
            return false;
        }

        if (header.getNumber() != parentHeader.getNumber() + 1) {
            logger.warn("Header number was not one greater than previous block.");
            return false;
        }

        if (!Arrays.equals(header.getParentHash(), parentHeader.getHash())) {
            logger.warn("Header parent hash was not equal to previous block hash.");
            return false;
        }

        if (header.getTimestamp() <= parentHeader.getTimestamp()) {
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
    public boolean validateTransactions(BlockHeader header, List<Transaction> transactions, Network network) {
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
    public boolean validateTransactions(BlockHeader header, Collection<Transaction> unvalidatedTransactions,
            List<Transaction> allTransactions, Network network) {

        // validate transactions
        if (!Key.isVerifyBatchSupported() || unvalidatedTransactions.size() < 3) {
            if (!unvalidatedTransactions.parallelStream().allMatch(tx -> tx.validate(network))) {
                return false;
            }
        } else {
            if (!unvalidatedTransactions.parallelStream().allMatch(tx -> tx.validate(network, false))) {
                return false;
            }

            if (!Key.verifyBatch(
                    unvalidatedTransactions.stream().map(Transaction::getHash).collect(Collectors.toList()),
                    unvalidatedTransactions.stream().map(Transaction::getSignature).collect(Collectors.toList()))) {
                return false;
            }
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
    public boolean validateResults(BlockHeader header, List<TransactionResult> results) {
        // validate results
        for (TransactionResult result : results) {
            if (result.getCode().isRejected()) {
                logger.warn("Transaction result does not match for " + result.toString());
                return false;
            }
        }

        // validate results root
        byte[] root = MerkleUtil.computeResultsRoot(results);
        boolean rootMatches = Arrays.equals(root, header.getResultsRoot());
        if (!rootMatches) {
            logger.warn("Merkle root does not match expected");
        }
        return rootMatches;
    }

    public static Amount getBlockReward(Block block, Config config) {

        Amount txsReward = block.getTransactions().stream().map(Transaction::getFee).reduce(ZERO, Amount::sum);
        Amount gasReward = getGasReward(block);

        return sum(sum(config.spec().getBlockReward(block.getNumber()), txsReward), gasReward);
    }

    /**
     * Retrieve the total gas award for the block
     *
     * @param block
     * @return
     */
    private static Amount getGasReward(Block block) {
        List<Transaction> transactions = block.getTransactions();
        List<TransactionResult> results = block.getResults();
        Amount sum = ZERO;
        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            TransactionResult result = results.get(i);
            sum = Amount.sum(sum, Amount.mul(transaction.getGasPrice(), result.getGasUsed()));
        }
        return sum;
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
     * Returns a shallow copy of the votes.
     *
     * @return
     */
    public List<Signature> getVotes() {
        return new ArrayList<>(votes);
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
        return header.toBytes();
    }

    /**
     * Serializes the block transactions into byte array.
     *
     * @return
     */
    public byte[] getEncodedTransactions() {
        return getEncodedTransactionsAndIndices().getLeft();
    }

    public Pair<byte[], List<Integer>> getEncodedTransactionsAndIndices() {
        List<Integer> indices = new ArrayList<>();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(transactions.size());
        for (Transaction transaction : transactions) {
            int index = enc.getWriteIndex();
            enc.writeBytes(transaction.toBytes());
            indices.add(index);
        }

        return Pair.of(enc.toBytes(), indices);
    }

    /**
     * Serializes the block transactions results into byte array.
     *
     * @return
     */
    public byte[] getEncodedResults() {
        return getEncodedResultsAndIndices().getLeft();
    }

    public Pair<byte[], List<Integer>> getEncodedResultsAndIndices() {
        List<Integer> indices = new ArrayList<>();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(results.size());
        for (TransactionResult result : results) {
            int index = enc.getWriteIndex();
            enc.writeBytes(result.toBytes());
            indices.add(index);
        }

        return Pair.of(enc.toBytes(), indices);
    }

    /**
     * Serializes the BFT votes into byte array.
     *
     * @return
     */
    public byte[] getEncodedVotes() {
        SimpleEncoder enc = new SimpleEncoder(4 + 4 + votes.size() * Signature.LENGTH);

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
        if (h == null) {
            throw new IllegalArgumentException("Block header can't be null");
        }
        if (t == null) {
            throw new IllegalArgumentException("Block transactions can't be null");
        }

        BlockHeader header = BlockHeader.fromBytes(h);

        SimpleDecoder dec = new SimpleDecoder(t);
        List<Transaction> transactions = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            transactions.add(Transaction.fromBytes(dec.readBytes()));
        }

        List<TransactionResult> results = new ArrayList<>();
        if (r != null) {
            dec = new SimpleDecoder(r);
            n = dec.readInt();
            for (int i = 0; i < n; i++) {
                results.add(TransactionResult.fromBytes(dec.readBytes()));
            }
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
