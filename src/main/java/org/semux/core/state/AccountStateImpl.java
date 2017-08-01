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

public class AccountStateImpl implements AccountState {

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

            private ByteArray keyBalance = ByteArray.of(makeKey(StateType.BALANCE, addr));
            private ByteArray keyLocked = ByteArray.of(makeKey(StateType.LOCKED, addr));
            private ByteArray keyNonce = ByteArray.of(makeKey(StateType.NONCE, addr));
            private ByteArray keyCode = ByteArray.of(makeKey(StateType.CODE, addr));

            private Account acc = (prev == null) ? null : prev.getAccount(addr);

            @Override
            public byte[] getAddress() {
                return addr;
            }

            @Override
            public long getBalance() {
                if (updates.containsKey(keyBalance)) {
                    return Bytes.toLong(updates.get(keyBalance));
                } else if (acc != null) {
                    return acc.getBalance();
                } else {
                    byte[] bytes = accountDB.get(keyBalance.getData());
                    return bytes == null ? 0 : Bytes.toLong(bytes);
                }
            }

            @Override
            public void setBalance(long balance) {
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
            public byte[] getStorage(long key) {
                ByteArray k = ByteArray.of(makeKeyForStorage(addr, key));
                if (updates.containsKey(k)) {
                    return updates.get(k);
                } else if (acc != null) {
                    return acc.getStorage(key);
                } else {
                    return accountDB.get(k.getData());
                }
            }

            @Override
            public void putStorage(long key, byte[] value) {
                ByteArray k = ByteArray.of(makeKeyForStorage(addr, key));
                updates.put(k, value);
            }

            @Override
            public void removeStorage(long key) {
                ByteArray k = ByteArray.of(makeKeyForStorage(addr, key));
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

    private byte[] makeKey(StateType type, byte[] addr) {
        byte[] k = Arrays.copyOf(addr, addr.length + 1);
        k[addr.length] = type.getCode();
        return k;
    }

    private byte[] makeKeyForStorage(byte[] addr, long key) {
        byte[] k = Arrays.copyOf(addr, addr.length + 1 + 8);
        k[addr.length] = StateType.STORAGE.getCode();
        System.arraycopy(Bytes.of(key), 0, k, addr.length + 1, 8);
        return k;
    }

    private enum StateType {
        BALANCE(0x00), LOCKED(0x01), NONCE(0x02), CODE(0x03), STORAGE(0x04);

        private byte code;

        StateType(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }
    }
}
