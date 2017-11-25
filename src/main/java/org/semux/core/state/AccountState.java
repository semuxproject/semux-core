/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

public interface AccountState {

    /**
     * Returns an account if exists.
     * 
     * @param addr
     * @return
     */
    Account getAccount(byte[] addr);

    /**
     * Increases the nonce of an account.
     * 
     * @param addr
     */
    void increaseNonce(byte[] addr);

    /**
     * Adjusts the available balance of an account.
     * 
     * @param addr
     * @param delta
     */
    void adjustAvailable(byte[] addr, long delta);

    /**
     * Adjusts the locked balance of an account.
     * 
     * @param addr
     * @param delta
     */
    void adjustLocked(byte[] addr, long delta);

    /**
     * Returns the code of an account.
     * 
     * @param addr
     */
    void getCode(byte[] addr);

    /**
     * Sets the code of an account.
     * 
     * @param addr
     * @param code
     */
    void setCode(byte[] addr, byte[] code);

    /**
     * Returns the value that is mapped to the key.
     * 
     * @param addr
     * @param key
     * @return the value if exists, otherwise null.
     */
    byte[] getStorage(byte[] addr, byte[] key);

    /**
     * Associates the specified value with the specified key.
     * 
     * @param addr
     * @param key
     * @param value
     */
    void putStorage(byte[] addr, byte[] key, byte[] value);

    /**
     * Remove a key value pair from the storage if exists.
     * 
     * @param addr
     * @param key
     */
    void removeStorage(byte[] addr, byte[] key);

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
}
