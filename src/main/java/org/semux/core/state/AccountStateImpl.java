/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.semux.core.Amount.sum;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.semux.core.Amount;
import org.semux.db.Database;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

/**
 * Account state implementation.
 * 
 * <pre>
 * account DB structure:
 * 
 * [0, address] => [account_object]
 * [1, address] => [code]
 * [2, address, storage_key] = [storage_value]
 * </pre>
 */
public class AccountStateImpl implements AccountState {

    protected static final byte TYPE_ACCOUNT = 0;
    protected static final byte TYPE_CODE = 1;
    protected static final byte TYPE_STORAGE = 2;

    protected Database accountDB;
    protected AccountStateImpl prev;
    // we will need to add persistence, but for integration/testing, seems fine to
    // punt
    private Map<ByteArray, Map<ByteArray, byte[]>> storage = new HashMap<>();

    /**
     * All updates, or deletes if the value is null.
     */
    protected final Map<ByteArray, byte[]> updates = new ConcurrentHashMap<>();

    /**
     * Create an {@link AccountState} that work directly on a database.
     * 
     * @param accountDB
     */
    public AccountStateImpl(Database accountDB) {
        this.accountDB = accountDB;
    }

    /**
     * Create an {@link AccountState} based on a previous AccountState.
     * 
     * @param prev
     */
    public AccountStateImpl(AccountStateImpl prev) {
        this.prev = prev;
        this.storage = new HashMap<>(prev.storage);
    }

    @Override
    public Account getAccount(byte[] address) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);
        Amount noAmount = Amount.ZERO;

        if (updates.containsKey(k)) {
            byte[] v = updates.get(k);
            return v == null ? new Account(address, noAmount, noAmount, 0) : Account.fromBytes(address, v);
        } else if (prev != null) {
            return prev.getAccount(address);
        } else {
            byte[] v = accountDB.get(k.getData());
            return v == null ? new Account(address, noAmount, noAmount, 0) : Account.fromBytes(address, v);
        }
    }

    @Override
    public long increaseNonce(byte[] address) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        long nonce = acc.getNonce() + 1;
        acc.setNonce(nonce);
        updates.put(k, acc.toBytes());
        return nonce;
    }

    @Override
    public void adjustAvailable(byte[] address, Amount delta) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        acc.setAvailable(sum(acc.getAvailable(), delta));
        updates.put(k, acc.toBytes());
    }

    @Override
    public void adjustLocked(byte[] address, Amount delta) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        acc.setLocked(sum(acc.getLocked(), delta));
        updates.put(k, acc.toBytes());
    }

    @Override
    public byte[] getCode(byte[] address) {
        Account acc = getAccount(address);
        return acc.getCode();
    }

    @Override
    public void setCode(byte[] address, byte[] code) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        acc.setCode(code);
        updates.put(k, acc.toBytes());
    }

    @Override
    public byte[] getStorage(byte[] address, byte[] key) {
        Map<ByteArray, byte[]> store = storage.get(ByteArray.of(address));
        if (store != null) {
            return store.get(ByteArray.of(key));
        }
        return null;
    }

    @Override
    public void putStorage(byte[] address, byte[] key, byte[] value) {
        ByteArray storeKey = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        Map<ByteArray, byte[]> store = acc.getStorage();
        store.put(ByteArray.of(key), value);
        updates.put(storeKey, acc.toBytes());
    }

    @Override
    public void removeStorage(byte[] address, byte[] key) {
        ByteArray storeKey = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        Map<ByteArray, byte[]> store = acc.getStorage();
        store.remove(ByteArray.of(key));
        updates.put(storeKey, acc.toBytes());
    }

    @Override
    public AccountState track() {
        return new AccountStateImpl(this);
    }

    @Override
    public void commit() {
        synchronized (updates) {
            if (prev == null) {
                for (Entry<ByteArray, byte[]> entry : updates.entrySet()) {
                    if (entry.getValue() == null) {
                        accountDB.delete(entry.getKey().getData());
                    } else {
                        accountDB.put(entry.getKey().getData(), entry.getValue());
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

    @Override
    public boolean exists(byte[] address) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);

        if (updates.containsKey(k)) {
            return true;
        } else if (prev != null) {
            return prev.exists(address);
        } else {
            byte[] v = accountDB.get(k.getData());
            return v != null;
        }
    }

    @Override
    public long setNonce(byte[] address, long nonce) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        acc.setNonce(nonce);
        updates.put(k, acc.toBytes());
        return nonce;
    }

    protected ByteArray getKey(byte type, byte[] address) {
        return ByteArray.of(Bytes.merge(type, address));
    }

    protected ByteArray getStorageKey(byte[] address, byte[] key) {
        byte[] buf = new byte[1 + address.length + key.length];
        buf[0] = TYPE_STORAGE;
        System.arraycopy(address, 0, buf, 1, address.length);
        System.arraycopy(key, 0, buf, 1 + address.length, key.length);

        return ByteArray.of(buf);
    }
}
