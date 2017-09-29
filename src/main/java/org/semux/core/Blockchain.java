/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.List;

import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;

public interface Blockchain {

    /**
     * Get the latest block.
     * 
     * @return
     */
    public Block getLatestBlock();

    /**
     * Get the hash of the latest block.
     * 
     * @return
     */
    public byte[] getLatestBlockHash();

    /**
     * Get the number of the latest block.
     * 
     * @return
     */
    public long getLatestBlockNumber();

    /**
     * Get block hash by number.
     * 
     * @param number
     * @return
     */
    public byte[] getBlockHash(long number);

    /**
     * Get genesis block.
     * 
     * @return
     */
    public Genesis getGenesis();

    /**
     * Get block by number.
     * 
     * @param number
     * @return
     */
    public Block getBlock(long number);

    /**
     * Get block by its hash.
     * 
     * @param hash
     * @return
     */
    public Block getBlock(byte[] hash);

    /**
     * Get block header by block number.
     * 
     * @param number
     * @return
     */
    public BlockHeader getBlockHeader(long number);

    /**
     * Get block header by block hash.
     * 
     * @param hash
     * @return
     */
    public BlockHeader getBlockHeader(byte[] hash);

    /**
     * Get transaction by its hash.
     * 
     * @param hash
     * @return
     */
    public Transaction getTransaction(byte[] hash);

    /**
     * Get the block number of the given transaction.
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
    public int getTotalTransactions(byte[] address);

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
     * Get the account state.
     * 
     * @return
     */
    public AccountState getAccountState();

    /**
     * Get the delegate state.
     * 
     * @return
     */
    public DelegateState getDeleteState();

    /**
     * Get the validator set based on current state.
     * 
     * @return the peerIds of validators
     */
    public List<String> getValidators();

    /**
     * Register a blockchain listener.
     * 
     * @param listener
     */
    public void addListener(BlockchainListener listener);
}
