/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;

public interface AccountState {

    /**
     * Returns an account if exists.
     * 
     * @param address
     * @return
     */
    Account getAccount(byte[] address);

    /**
     * Increases the nonce of an account.
     * 
     * @param address
     */
    long increaseNonce(byte[] address);

    /**
     * Adjusts the available balance of an account.
     * 
     * @param address
     * @param delta
     */
    void adjustAvailable(byte[] address, Amount delta);

    /**
     * Adjusts the locked balance of an account.
     * 
     * @param address
     * @param delta
     */
    void adjustLocked(byte[] address, Amount delta);

    /**
     * Returns the code of an account.
     * 
     * @param address
     */
    byte[] getCode(byte[] address);

    /**
     * Sets the code of an account.
     * 
     * @param address
     * @param code
     */
    void setCode(byte[] address, byte[] code);

    /**
     * Returns the value that is mapped to the key.
     * 
     * @param address
     * @param key
     * @return the value if exists, otherwise null.
     */
    byte[] getStorage(byte[] address, byte[] key);

    /**
     * Associates the specified value with the specified key.
     * 
     * @param address
     * @param key
     * @param value
     */
    void putStorage(byte[] address, byte[] key, byte[] value);

    /**
     * Remove a key value pair from the storage if exists.
     * 
     * @param address
     * @param key
     */
    void removeStorage(byte[] address, byte[] key);

    /**
     * Makes a snapshot and starts tracking further updates.
     */
    AccountState track();

    /**
     * Commits all updates since last snapshot.
     */
    void commit();

    /**
     * Reverts all updates since last snapshot.
     */
    void rollback();

    /**
     * check if an account exists
     * 
     * @param address
     *            address
     * @return exists
     */
    boolean exists(byte[] address);

    /**
     * set the nonce to a given value.
     *
     * @param address
     *            address
     * @param nonce
     *            nonce
     * @return nonce
     */
    long setNonce(byte[] address, long nonce);
}
