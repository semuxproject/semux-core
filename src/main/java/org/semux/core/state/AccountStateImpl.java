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

import org.semux.core.Account;
import org.semux.db.KVDB;
import org.semux.utils.ByteArray;
import org.semux.utils.Bytes;

/**
 * Account state implementation.
 * 
 * <pre>
 * account DB structure:
 * 
 * [address, 0] => [balance]
 * [address, 1] => [locked]
 * [address, 2] => [nonce]
 * [address, 3] => [code]
 * [address, 4, storage_key] = [storage_value]
 * </pre>
 */
public class AccountStateImpl implements AccountState {

    private static byte BALANCE = 0;
    private static byte LOCKED = 1;
    private static byte NONCE = 2;
    private static byte CODE = 3;
    private static byte STORAGE = 4;

    private KVDB accountDB;
    private AccountStateImpl prev;

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
        return new Account() {
            private ByteArray keyBalance = getKey(addr, BALANCE);
            private ByteArray keyLocked = getKey(addr, LOCKED);
            private ByteArray keyNonce = getKey(addr, NONCE);
            private ByteArray keyCode = getKey(addr, CODE);

            private Account acc = (prev == null) ? null : prev.getAccount(addr);

            @Override
            public byte[] getAddress() {
                return addr;
            }

            @Override
            public long getAvailable() {
                if (updates.containsKey(keyBalance)) {
                    return Bytes.toLong(updates.get(keyBalance));
                } else if (acc != null) {
                    return acc.getAvailable();
                } else {
                    byte[] bytes = accountDB.get(keyBalance.getData());
                    return bytes == null ? 0 : Bytes.toLong(bytes);
                }
            }

            @Override
            public void setAvailable(long balance) {
                updates.put(keyBalance, Bytes.of(balance));
            }

            @Override
            public long getLocked() {
                if (updates.containsKey(keyLocked)) {
                    return Bytes.toLong(updates.get(keyLocked));
                } else if (acc != null) {
                    return acc.getLocked();
                } else {
                    byte[] bytes = accountDB.get(keyLocked.getData());
                    return bytes == null ? 0 : Bytes.toLong(bytes);
                }
            }

            @Override
            public void setLocked(long locked) {
                updates.put(keyLocked, Bytes.of(locked));
            }

            @Override
            public long getNonce() {
                if (updates.containsKey(keyNonce)) {
                    return Bytes.toLong(updates.get(keyNonce));
                } else if (acc != null) {
                    return acc.getNonce();
                } else {
                    byte[] bytes = accountDB.get(keyNonce.getData());
                    return bytes == null ? 0 : Bytes.toLong(bytes);
                }
            }

            @Override
            public void setNonce(long nonce) {
                updates.put(keyNonce, Bytes.of(nonce));
            }

            @Override
            public byte[] getCode() {
                if (updates.containsKey(keyCode)) {
                    return updates.get(keyCode);
                } else if (acc != null) {
                    return acc.getCode();
                } else {
                    return accountDB.get(keyCode.getData());
                }
            }

            @Override
            public void setCode(byte[] code) {
                updates.put(keyCode, code);
            }

            @Override
            public byte[] getStorage(byte[] key) {
                ByteArray k = getStorageKey(addr, key);
                if (updates.containsKey(k)) {
                    return updates.get(k);
                } else if (acc != null) {
                    return acc.getStorage(key);
                } else {
                    return accountDB.get(k.getData());
                }
            }

            @Override
            public void putStorage(byte[] key, byte[] value) {
                ByteArray k = getStorageKey(addr, key);
                updates.put(k, value);
            }

            @Override
            public void removeStorage(byte[] key) {
                ByteArray k = getStorageKey(addr, key);
                updates.put(k, null);
            }
        };
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

    private ByteArray getKey(byte[] addr, byte type) {
        byte[] k = Arrays.copyOf(addr, addr.length + 1);
        k[addr.length] = type;

        return ByteArray.of(k);
    }

    private ByteArray getStorageKey(byte[] addr, byte[] key) {
        byte[] k = Arrays.copyOf(addr, addr.length + 1 + key.length);
        k[addr.length] = STORAGE;
        System.arraycopy(key, 0, k, addr.length + 1, key.length);

        return ByteArray.of(k);
    }
}
