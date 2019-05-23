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

import org.semux.Network;
import org.semux.config.Config;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.Key.Signature;
import org.semux.util.MerkleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a block in the blockchain.
 */
public class Block {

    public enum BlockPart {
        HEADER(1 << 0), TRANSACTIONS(1 << 1), RECEIPTS(1 << 2), VOTES(1 << 3);

        private int code;

        BlockPart(int code) {
            this.code = code;
        }

        public static int parts(BlockPart... parts) {
            int result = 0;
            for (BlockPart part : parts) {
                result |= part.code;
            }
            return result;
        }

        public List<BlockPart> parts(int parts) {
            List<BlockPart> result = new ArrayList<>();
            // NOTE: values() returns an array containing all of the values of the enum type
            // in the order they are declared.
            for (BlockPart bp : BlockPart.values()) {
                if ((parts & bp.code) != 0) {
                    result.add(bp);
                }
            }

            return result;
        }
    }

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
     * @param previous
     * @return
     */
    public boolean validateHeader(BlockHeader previous, BlockHeader header) {
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
        Amount reward = sum(sum(config.getBlockReward(block.getNumber()), txsReward), gasReward);

        return reward;
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
        long sum = 0;
        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            TransactionResult result = results.get(i);
            sum += (transaction.getGasPrice() * result.getGasUsed());
        }
        return Amount.Unit.NANO_SEM.of(sum);
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

    @Override
    public String toString() {
        return "Block [number = " + getNumber() + ", view = " + getView() + ", hash = " + Hex.encode(getHash())
                + ", # txs = " + transactions.size() + ", # votes = " + votes.size() + "]";
    }

}
