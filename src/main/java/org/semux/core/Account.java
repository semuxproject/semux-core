/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

public interface Account {
    /**
     * Get the address of this account.
     * 
     * @return
     */
    public byte[] getAddress();

    /**
     * Get the balance of this account.
     * 
     * @return
     */
    public long getBalance();

    /**
     * Set the balance of this account.
     * 
     * @param balance
     */
    public void setBalance(long balance);

    /**
     * Get the locked balance of this account.
     * 
     * @return
     */
    public long getLocked();

    /**
     * Set the locked balance of this account.
     * 
     * @param locked
     */
    public void setLocked(long locked);

    /**
     * Get the nonce of this account.
     * 
     * @return
     */
    public long getNonce();

    /**
     * Set the nonce of this account.
     * 
     * @param nonce
     */
    public void setNonce(long nonce);

    /**
     * Get the code of this account.
     * 
     * @return code byte array, or null if not exist
     */
    public byte[] getCode();

    /**
     * Set the code of this account.
     * 
     * @param code
     */
    public void setCode(byte[] code);

    /**
     * Get the value that is mapped to the key.
     * 
     * @param key
     * @return the value if exists, otherwise null.
     */
    public byte[] getStorage(byte[] key);

    /**
     * Associates the specified value with the specified key.
     * 
     * @param key
     * @param value
     */
    public void putStorage(byte[] key, byte[] value);

    /**
     * Remove a key value pair from the storage if exists.
     * 
     * @param key
     */
    public void removeStorage(byte[] key);
}
