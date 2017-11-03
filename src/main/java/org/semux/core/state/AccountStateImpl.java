/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.semux.db.KVDB;
import org.semux.utils.ByteArray;

/**
 * Account state implementation.
 * 
 * <pre>
 * account DB structure:
 * 
 * [address, 0] => [account_object]
 * [address, 1] => [code+]
 * [address, 2, storage_key] = [storage_value]
 * </pre>
 */
public class AccountStateImpl implements AccountState {

    protected static byte TYPE_ACCOUNT = 0;
    protected static byte TYPE_CODE = 1;
    protected static byte TYPE_STORAGE = 2;

    protected KVDB accountDB;
    protected AccountStateImpl prev;

    /**
     * All updates, or deletes if the value is null.
     */
    protected Map<ByteArray, byte[]> updates = new ConcurrentHashMap<>();

    /**
     * Create an AcccountState that work directly on a database.
     * 
     * @param accountDB
     */
    public AccountStateImpl(KVDB accountDB) {
        this.accountDB = accountDB;
    }

    /**
     * Create an AcccountState based on a previous AccountState.
     * 
     * @param prev
     */
    public AccountStateImpl(AccountStateImpl prev) {
        this.prev = prev;
    }

    @Override
    public Account getAccount(byte[] addr) {
        ByteArray k = getKey(addr, TYPE_ACCOUNT);

        if (updates.containsKey(k)) {
            byte[] v = updates.get(k);
            return v == null ? new Account(addr, 0, 0, 0) : Account.fromBytes(addr, v);
        } else if (prev != null) {
            return prev.getAccount(addr);
        } else {
            byte[] v = accountDB.get(k.getData());
            return v == null ? new Account(addr, 0, 0, 0) : Account.fromBytes(addr, v);
        }
    }

    @Override
    public void increaseNonce(byte[] addr) {
        ByteArray k = getKey(addr, TYPE_ACCOUNT);

        Account acc = getAccount(addr);
        acc.setNonce(acc.getNonce() + 1);
        updates.put(k, acc.toBytes());
    }

    @Override
    public void adjustAvailable(byte[] addr, long delta) {
        ByteArray k = getKey(addr, TYPE_ACCOUNT);

        Account acc = getAccount(addr);
        acc.setAvailable(acc.getAvailable() + delta);
        updates.put(k, acc.toBytes());
    }

    @Override
    public void adjustLocked(byte[] addr, long delta) {
        ByteArray k = getKey(addr, TYPE_ACCOUNT);

        Account acc = getAccount(addr);
        acc.setLocked(acc.getLocked() + delta);
        updates.put(k, acc.toBytes());
    }

    @Override
    public void getCode(byte[] addr) {
        throw new UnsupportedOperationException("getCode() is not yet supported");
    }

    @Override
    public void setCode(byte[] addr, byte[] code) {
        throw new UnsupportedOperationException("setCode() is not yet supported");
    }

    @Override
    public byte[] getStorage(byte[] addr, byte[] key) {
        throw new UnsupportedOperationException("getStorage() is not yet supported");
    }

    @Override
    public void putStorage(byte[] addr, byte[] key, byte[] value) {
        throw new UnsupportedOperationException("putStorage() is not yet supported");
    }

    @Override
    public void removeStorage(byte[] addr, byte[] key) {
        throw new UnsupportedOperationException("removeStorage() is not yet yetsupported");
    }

    @Override
    public AccountState track() {
        return new AccountStateImpl(this);
    }

    @Override
    public void commit() {
        synchronized (updates) {
            if (prev == null) {
                for (ByteArray k : updates.keySet()) {
                    byte[] v = updates.get(k);
                    if (v == null) {
                        accountDB.delete(k.getData());
                    } else {
                        accountDB.put(k.getData(), v);
                    }
                }
            } else {
                for (Entry<ByteArray, byte[]> e : updates.entrySet()) {
                    prev.updates.put(e.getKey(), e.getValue());
                }
            }

            updates.clear();
        }
    }

    @Override
    public void rollback() {
        updates.clear();
    }

    protected ByteArray getKey(byte[] addr, byte type) {
        byte[] k = Arrays.copyOf(addr, addr.length + 1);
        k[addr.length] = type;

        return ByteArray.of(k);
    }

    protected ByteArray getStorageKey(byte[] addr, byte[] key) {
        byte[] k = Arrays.copyOf(addr, addr.length + 1 + key.length);
        k[addr.length] = TYPE_STORAGE;
        System.arraycopy(key, 0, k, addr.length + 1, key.length);

        return ByteArray.of(k);
    }
}
