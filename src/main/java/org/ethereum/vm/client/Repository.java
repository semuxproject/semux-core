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
     * Creates an account.
     *
     * @param address
     *            the account address
     */
    void createAccount(byte[] address);

    /**
     * Checks whether an account exists.
     *
     * @param address
     *            the account address
     * @return true if account exist, false otherwise
     */
    boolean isExist(byte[] address);

    /**
     * Deletes an account.
     *
     * @param address
     *            the account address
     */
    void delete(byte[] address);

    /**
     * Increases the account nonce of the given account by one.
     *
     * @param address
     *            the account address
     * @return new value of the nonce
     */
    long increaseNonce(byte[] address);

    /**
     * Sets the account nonce of the given account
     *
     * @param address
     *            the account address
     * @param nonce
     *            new nonce
     * @return new value of the nonce
     */
    long setNonce(byte[] address, long nonce);

    /**
     * Returns current nonce of a given account
     *
     * @param address
     *            the account address
     * @return value of the nonce
     */
    long getNonce(byte[] address);

    /**
     * Stores code associated with an account
     *
     * @param address
     *            the account address
     * @param code
     *            that will be associated with this account
     */
    void saveCode(byte[] address, byte[] code);

    /**
     * Retrieves the code associated with an account
     *
     * @param address
     *            the account address
     * @return code in byte-array format
     */
    byte[] getCode(byte[] address);

    /**
     * Puts a value in storage of an account at a given key
     *
     * @param address
     *            the account address
     * @param key
     *            of the data to store
     * @param value
     *            is the data to store
     */
    void putStorageRow(byte[] address, DataWord key, DataWord value);

    /**
     * Retrieves storage value from an account for a given key
     *
     * @param address
     *            the account address
     * @param key
     *            associated with this value
     * @return the value, or NULL if not exist
     */
    DataWord getStorageRow(byte[] address, DataWord key);

    /**
     * Retrieves balance of an account
     *
     * @param address
     *            the account address
     * @return balance of the account as a <code>BigInteger</code> value
     */
    BigInteger getBalance(byte[] address);

    /**
     * Add value to the balance of an account
     *
     * @param address
     *            the account address
     * @param value
     *            to be added
     * @return new balance of the account
     */
    BigInteger addBalance(byte[] address, BigInteger value);

    /**
     * Save a snapshot and start tracking future changes
     *
     * @return the tracker repository
     */
    Repository startTracking();

    /**
     * Stores all the temporary changes made to the repository in the actual
     * database
     */
    void commit();

    /**
     * Undoes all the changes made so far to a snapshot of the repository
     */
    void rollback();
}
