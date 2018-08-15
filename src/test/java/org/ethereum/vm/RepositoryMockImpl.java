/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.ethereum.vm.client.Repository;
import org.ethereum.vm.util.ByteArrayWrapper;

public class RepositoryMockImpl implements Repository {

    private class Account {
        long nonce = 0;
        BigInteger balance = BigInteger.ZERO;
        byte[] code = new byte[0];
        Map<DataWord, DataWord> storage = new HashMap();
    }

    private Map<ByteArrayWrapper, Account> accounts = new HashMap();

    private Account getAccount(byte[] address) {
        return accounts.computeIfAbsent(new ByteArrayWrapper(address), k -> new Account());
    }

    @Override
    public void createAccount(byte[] address) {
        getAccount(address);
    }

    @Override
    public boolean isExist(byte[] address) {
        return accounts.containsKey(new ByteArrayWrapper(address));
    }

    @Override
    public void delete(byte[] address) {
        accounts.remove(new ByteArrayWrapper(address));
    }

    @Override
    public long increaseNonce(byte[] address) {
        return getAccount(address).nonce += 1;
    }

    @Override
    public long setNonce(byte[] address, long nonce) {
        return (getAccount(address).nonce = nonce);
    }

    @Override
    public long getNonce(byte[] address) {
        return getAccount(address).nonce;
    }

    @Override
    public void saveCode(byte[] address, byte[] code) {
        getAccount(address).code = code;
    }

    @Override
    public byte[] getCode(byte[] address) {
        return getAccount(address).code;
    }

    @Override
    public void putStorageRow(byte[] address, DataWord key, DataWord value) {
        getAccount(address).storage.put(key, value);
    }

    @Override
    public DataWord getStorageRow(byte[] address, DataWord key) {
        return getAccount(address).storage.get(key);
    }

    @Override
    public BigInteger getBalance(byte[] address) {
        return getAccount(address).balance;
    }

    @Override
    public BigInteger addBalance(byte[] address, BigInteger value) {
        return getAccount(address).balance = getAccount(address).balance.add(value);
    }

    @Override
    public Repository startTracking() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void rollback() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
