/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.List;

import org.semux.core.BlockchainImpl.ValidatorStats;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;

public interface Blockchain {

    /**
     * Returns the latest block.
     * 
     * @return
     */
    public Block getLatestBlock();

    /**
     * Returns the hash of the latest block.
     * 
     * @return
     */
    public byte[] getLatestBlockHash();

    /**
     * Returns the number of the latest block.
     * 
     * @return
     */
    public long getLatestBlockNumber();

    /**
     * Returns block number by hash.
     * 
     * @param hash
     * @return
     */
    public long getBlockNumber(byte[] hash);

    /**
     * Returns genesis block.
     * 
     * @return
     */
    public Genesis getGenesis();

    /**
     * Returns block by number.
     * 
     * @param number
     * @return
     */
    public Block getBlock(long number);

    /**
     * Returns block by its hash.
     * 
     * @param hash
     * @return
     */
    public Block getBlock(byte[] hash);

    /**
     * Returns block header by block number.
     * 
     * @param number
     * @return
     */
    public BlockHeader getBlockHeader(long number);

    /**
     * Returns block header by block hash.
     * 
     * @param hash
     * @return
     */
    public BlockHeader getBlockHeader(byte[] hash);

    /**
     * Returns transaction by its hash.
     * 
     * @param hash
     * @return
     */
    public Transaction getTransaction(byte[] hash);

    /**
     * Returns transaction result.
     * 
     * @param hash
     * @return
     */
    public TransactionResult getTransactionResult(byte[] hash);

    /**
     * Returns the block number of the given transaction.
     * 
     * @param hash
     * @return
     */
    public long getTransactionBlockNumber(byte[] hash);

    /**
     * Returns the total number of transactions from/to the given address.
     * 
     * @param address
     *            account address
     * @return
     */
    public int getTransactionCount(byte[] address);

    /**
     * Returns transactions from/to an address.
     * 
     * @param address
     *            account address
     * @param from
     *            transaction index from
     * @param tio
     *            transaction index to
     * @return
     */
    public List<Transaction> getTransactions(byte[] address, int from, int to);

    /**
     * Add a block to the chain.
     * 
     * @param block
     */
    public void addBlock(Block block);

    /**
     * Returns account state.
     * 
     * @return
     */
    public AccountState getAccountState();

    /**
     * Returns delegate state.
     * 
     * @return
     */
    public DelegateState getDelegateState();

    /**
     * Returns the validator set based on current state.
     * 
     * @return the peerIds of validators
     */
    public List<String> getValidators();

    /**
     * Returns the statistics of a validator.
     * 
     * @param address
     * @return
     */
    public ValidatorStats getValidatorStats(byte[] address);

    /**
     * Register a blockchain listener.
     * 
     * @param listener
     */
    public void addListener(BlockchainListener listener);
}
