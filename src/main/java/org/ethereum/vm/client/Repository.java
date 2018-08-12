/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm.client;

import java.math.BigInteger;

import org.ethereum.vm.DataWord;

public interface Repository {

    /**
     * @param addr
     *            - account to check
     * @return - true if account exist, false otherwise
     */
    boolean isExist(byte[] addr);

    /**
     * Deletes the account
     *
     * @param addr
     *            of the account
     */
    void delete(byte[] addr);

    /**
     * Increase the account nonce of the given account by one
     *
     * @param addr
     *            of the account
     * @return new value of the nonce
     */
    long increaseNonce(byte[] addr);

    /**
     * Sets the account nonce of the given account
     *
     * @param addr
     *            of the account
     * @param nonce
     *            new nonce
     * @return new value of the nonce
     */
    long setNonce(byte[] addr, long nonce);

    /**
     * Get current nonce of a given account
     *
     * @param addr
     *            of the account
     * @return value of the nonce
     */
    long getNonce(byte[] addr);

    /**
     * Store code associated with an account
     *
     * @param addr
     *            for the account
     * @param code
     *            that will be associated with this account
     */
    void saveCode(byte[] addr, byte[] code);

    /**
     * Retrieve the code associated with an account
     *
     * @param addr
     *            of the account
     * @return code in byte-array format
     */
    byte[] getCode(byte[] addr);

    /**
     * Put a value in storage of an account at a given key
     *
     * @param addr
     *            of the account
     * @param key
     *            of the data to store
     * @param value
     *            is the data to store
     */
    void putStorageRow(byte[] addr, DataWord key, DataWord value);

    /**
     * Retrieve storage value from an account for a given key
     *
     * @param addr
     *            of the account
     * @param key
     *            associated with this value
     * @return data in the form of a <code>DataWord</code>
     */
    DataWord getStorageRow(byte[] addr, DataWord key);

    /**
     * Retrieve balance of an account
     *
     * @param addr
     *            of the account
     * @return balance of the account as a <code>BigInteger</code> value
     */
    BigInteger getBalance(byte[] addr);

    /**
     * Add value to the balance of an account
     *
     * @param addr
     *            of the account
     * @param value
     *            to be added
     * @return new balance of the account
     */
    BigInteger addBalance(byte[] addr, BigInteger value);

    /**
     * Save a snapshot and start tracking future changes
     *
     * @return the tracker repository
     */
    Repository startTracking();

    /**
     * Store all the temporary changes made to the repository in the actual database
     */
    void commit();

    /**
     * Undo all the changes made so far to a snapshot of the repository
     */
    void rollback();
}
