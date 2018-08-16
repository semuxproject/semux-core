/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm.client;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.util.ByteArrayWrapper;

public class RepositoryMockImpl implements Repository {

    private Map<ByteArrayWrapper, Account> accounts = new HashMap();
    private RepositoryMockImpl parent;

    public RepositoryMockImpl() {
        this(null);
    }

    public RepositoryMockImpl(RepositoryMockImpl parent) {
        this.parent = parent;
    }

    /**
     * Returns an account if exists.
     *
     * @param address
     *            the account address
     * @return an account if exists, NULL otherwise
     */
    protected Account getAccount(byte[] address) {
        ByteArrayWrapper key = new ByteArrayWrapper(address);

        if (accounts.containsKey(key)) {
            return accounts.get(key);
        } else if (parent != null && parent.isExist(address)) {
            Account account = parent.getAccount(address);
            Account accountTrack = new Account(account);
            accounts.put(key, accountTrack);
            return accountTrack;
        } else {
            return null;
        }
    }

    @Override
    public boolean isExist(byte[] address) {
        ByteArrayWrapper key = new ByteArrayWrapper(address);

        if (accounts.containsKey(new ByteArrayWrapper(address))) {
            return accounts.get(key) != null;
        } else if (parent != null) {
            return parent.isExist(address);
        } else {
            return false;
        }
    }

    @Override
    public void createAccount(byte[] address) {
        if (!isExist(address)) {
            accounts.put(new ByteArrayWrapper(address), new Account());
        }
    }

    @Override
    public void delete(byte[] address) {
        accounts.put(new ByteArrayWrapper(address), null);
    }

    @Override
    public long increaseNonce(byte[] address) {
        createAccount(address);
        return getAccount(address).nonce += 1;
    }

    @Override
    public long setNonce(byte[] address, long nonce) {
        createAccount(address);
        return (getAccount(address).nonce = nonce);
    }

    @Override
    public long getNonce(byte[] address) {
        Account account = getAccount(address);
        return account == null ? 0 : account.nonce;
    }

    @Override
    public void saveCode(byte[] address, byte[] code) {
        createAccount(address);
        getAccount(address).code = code;
    }

    @Override
    public byte[] getCode(byte[] address) {
        Account account = getAccount(address);
        return account == null ? null : account.code;
    }

    @Override
    public void putStorageRow(byte[] address, DataWord key, DataWord value) {
        createAccount(address);
        getAccount(address).storage.put(key, value);
    }

    @Override
    public DataWord getStorageRow(byte[] address, DataWord key) {
        Account account = getAccount(address);
        return account == null ? null : account.storage.get(key);
    }

    @Override
    public BigInteger getBalance(byte[] address) {
        Account account = getAccount(address);
        return account == null ? null : account.balance;
    }

    @Override
    public BigInteger addBalance(byte[] address, BigInteger value) {
        createAccount(address);
        Account account = getAccount(address);
        return account.balance = account.balance.add(value);
    }

    @Override
    public RepositoryMockImpl startTracking() {
        return new RepositoryMockImpl(this);
    }

    @Override
    public void commit() {
        if (parent != null) {
            parent.accounts.putAll(accounts);
        }
    }

    @Override
    public void rollback() {
        accounts.clear();
    }

    static class Account {
        public long nonce = 0;
        public BigInteger balance = BigInteger.ZERO;
        public byte[] code = new byte[0];
        public Map<DataWord, DataWord> storage = new HashMap();

        public Account() {
        }

        public Account(Account parent) {
            this.nonce = parent.nonce;
            this.balance = parent.balance;
            this.code = parent.code;
            this.storage = new HashMap<>(parent.storage);
        }
    }
}
