/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;

public interface SemuxRuntime {

    // =============================
    // Transaction information
    // =============================

    /**
     * Get the address of this contract.
     * 
     * @return
     */
    public byte[] getAddress();

    /**
     * Get sender address of this transaction
     * 
     * @return
     */
    public byte[] getSender();

    /**
     * Get the value of this transaction
     * 
     * @return
     */
    public long getValue();

    /**
     * Get the data of this transaction
     * 
     * @return
     */
    public byte[] getData();

    // =============================
    // Block information
    // =============================

    /**
     * Get the hash of last block.
     * 
     * @return
     */
    public byte[] getBockHash();

    /**
     * Get the number of last block.
     * 
     * @return
     */
    public long getBlockNumber();

    /**
     * Get the coinbase of last block.
     * 
     * @return
     */
    public byte[] getBlockCoinbase();

    /**
     * Get the timestamp of last block.
     * 
     * @return
     */
    public long getBlockTimestamp();

    // =============================
    // Extra
    // =============================

    /**
     * Get account state.
     * 
     * @return
     */
    public AccountState getAccountState();

    /**
     * Get the transaction result which stores output and logs.
     * 
     * @return
     */
    public TransactionResult result();

    /**
     * Send a transaction to the specified address.
     * 
     * @param to
     * @param value
     * @param data
     */
    public void send(byte[] to, long value, byte[] data);
}
