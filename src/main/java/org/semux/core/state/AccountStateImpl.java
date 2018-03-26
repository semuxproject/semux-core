/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.semux.core.Amount.sum;

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
    public void increaseNonce(byte[] address) {
        ByteArray k = getKey(TYPE_ACCOUNT, address);

        Account acc = getAccount(address);
        acc.setNonce(acc.getNonce() + 1);
        updates.put(k, acc.toBytes());
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
    public void getCode(byte[] address) {
        throw new UnsupportedOperationException("getCode() is not yet supported");
    }

    @Override
    public void setCode(byte[] address, byte[] code) {
        throw new UnsupportedOperationException("setCode() is not yet supported");
    }

    @Override
    public byte[] getStorage(byte[] address, byte[] key) {
        throw new UnsupportedOperationException("getStorage() is not yet supported");
    }

    @Override
    public void putStorage(byte[] address, byte[] key, byte[] value) {
        throw new UnsupportedOperationException("putStorage() is not yet supported");
    }

    @Override
    public void removeStorage(byte[] address, byte[] key) {
        throw new UnsupportedOperationException("removeStorage() is not yet yet supported");
    }

    @Override
    public AccountState track() {
        return new AccountStateImpl(this);
    }

    @Override
    public void commit() {
        synchronized (updates) {
            if (prev == null) {
                for (Map.Entry<ByteArray, byte[]> entry : updates.entrySet()) {
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
