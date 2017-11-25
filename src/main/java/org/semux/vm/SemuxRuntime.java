/**
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
     * Returns the address of this contract.
     * 
     * @return
     */
    byte[] getAddress();

    /**
     * Returns sender address of this transaction
     * 
     * @return
     */
    byte[] getSender();

    /**
     * Returns the value of this transaction
     * 
     * @return
     */
    long getValue();

    /**
     * Returns the data of this transaction
     * 
     * @return
     */
    byte[] getData();

    // =============================
    // Block information
    // =============================

    /**
     * Returns the hash of last block.
     * 
     * @return
     */
    byte[] getBockHash();

    /**
     * Returns the number of last block.
     * 
     * @return
     */
    long getBlockNumber();

    /**
     * Returns the coinbase of last block.
     * 
     * @return
     */
    byte[] getBlockCoinbase();

    /**
     * Returns the timestamp of last block.
     * 
     * @return
     */
    long getBlockTimestamp();

    // =============================
    // Extra
    // =============================

    /**
     * Get account state.
     * 
     * @return
     */
    AccountState getAccountState();

    /**
     * Returns the transaction result which stores output and logs.
     * 
     * @return
     */
    TransactionResult result();

    /**
     * Sends a transaction to the specified address.
     * 
     * @param to
     * @param value
     * @param data
     */
    void send(byte[] to, long value, byte[] data);
}
