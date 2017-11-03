/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

public interface Account {
    /**
     * Returns the address of this account.
     * 
     * @return
     */
    public byte[] getAddress();

    /**
     * Returns the available balance of this account.
     * 
     * @return
     */
    public long getAvailable();

    /**
     * Sets the available balance of this account.
     * 
     * @param balance
     */
    public void setAvailable(long balance);

    /**
     * Returns the locked balance of this account.
     * 
     * @return
     */
    public long getLocked();

    /**
     * Sets the locked balance of this account.
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
     * Returns the code of this account.
     * 
     * @return code byte array, or null if not exist
     */
    public byte[] getCode();

    /**
     * Sets the code of this account.
     * 
     * @param code
     */
    public void setCode(byte[] code);

    /**
     * Returns the value that is mapped to the key.
     * 
     * @param key
     * @return the value if exists, otherwise null.
     */
    public byte[] getStorage(byte[] key);

    /**
     * Sets the specified value for the given key.
     * 
     * @param key
     * @param value
     */
    public void putStorage(byte[] key, byte[] value);

    /**
     * Remove a key-value pair from the storage if exists.
     * 
     * @param key
     */
    public void removeStorage(byte[] key);
}
