/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.semux.core.Amount.sum;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.semux.core.Amount;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.db.BatchManager;
import org.semux.db.BatchName;
import org.semux.db.BatchOperation;
import org.semux.db.Database;
import org.semux.db.DatabasePrefixesV2;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class AccountStateImplV2 implements Cloneable, AccountStateV2 {

    protected static final Logger logger = LoggerFactory.getLogger(AccountStateImplV2.class);

    private final Database database;
    private final BatchManager batchManager;
    protected AccountStateImplV2 prev;

    /**
     * All updates, or deletes if the value is null.
     */
    protected final Map<ByteArray, byte[]> updates = new ConcurrentHashMap<>();

    protected final Set<ByteArray> pendingAccounts = new ConcurrentSkipListSet<>();

    /**
     * Create an {@link AccountState} that work directly on a database.
     *
     */
    public AccountStateImplV2(Database database, BatchManager batchManager) {
        this.database = database;
        this.batchManager = batchManager;
    }

    /**
     * Create an {@link AccountState} based on a previous AccountState.
     *
     * @param prev
     */
    private AccountStateImplV2(AccountStateImplV2 prev) {
        this(prev.database, prev.batchManager);
        this.prev = prev;
    }

    @Override
    public Account getAccount(byte[] address) {
        assert (address.length == 20);
        ByteArray k = getAddressToBinaryKey(address);
        Amount noAmount = Amount.ZERO;

        if (updates.containsKey(k)) {
            byte[] v = updates.get(k);
            return v == null ? new AccountV1(address, noAmount, noAmount, 0)
                    : new AccountDecoderV2().decode(address, v);
        } else if (prev != null) {
            return prev.getAccount(address);
        } else {
            byte[] v = database.get(k.getData());
            return v == null ? new AccountV1(address, noAmount, noAmount, 0)
                    : new AccountDecoderV2().decode(address, v);
        }
    }

    @Override
    public long increaseNonce(byte[] Abyte) {
        assert (Abyte.length == 32);
        byte[] address = Key.Address.fromAbyte(Abyte);
        ByteArray k = getAddressToBinaryKey(address);

        Account acc = getAccount(address);
        long nonce = acc.getNonce() + 1;
        acc.setNonce(nonce);
        updates.put(k, new AccountEncoderV2().encode(acc));

        if (nonce == 1) {
            pendingAccounts.add(ByteArray.of(Abyte));
        }

        return nonce;
    }

    @Override
    public void adjustAvailable(byte[] address, Amount delta) {
        ByteArray k = getAddressToBinaryKey(address);

        Account acc = getAccount(address);
        acc.setAvailable(sum(acc.getAvailable(), delta));
        updates.put(k, new AccountEncoderV2().encode(acc));
    }

    @Override
    public void adjustLocked(byte[] address, Amount delta) {
        ByteArray k = getAddressToBinaryKey(address);

        Account acc = getAccount(address);
        acc.setLocked(sum(acc.getLocked(), delta));
        updates.put(k, new AccountEncoderV2().encode(acc));
    }

    @Override
    public byte[] getCode(byte[] address) {
        ByteArray k = getAddressToCodeKey(address);

        if (updates.containsKey(k)) {
            return updates.get(k);
        } else if (prev != null) {
            return prev.getCode(address);
        } else {
            return database.get(k.getData());
        }
    }

    @Override
    public void setCode(byte[] address, byte[] code) {
        ByteArray k = getAddressToCodeKey(address);
        updates.put(k, code);
    }

    @Override
    public byte[] getStorage(byte[] address, byte[] key) {
        ByteArray k = getAddressToStorageKey(address, key);

        if (updates.containsKey(k)) {
            return updates.get(k);
        } else if (prev != null) {
            return prev.getStorage(address, key);
        } else {
            return database.get(k.getData());
        }
    }

    @Override
    public void putStorage(byte[] address, byte[] key, byte[] value) {
        ByteArray storeKey = getAddressToStorageKey(address, key);
        updates.put(storeKey, value);
    }

    @Override
    public void removeStorage(byte[] address, byte[] key) {
        ByteArray storeKey = getAddressToStorageKey(address, key);
        updates.put(storeKey, null);
    }

    @Override
    public AccountState track() {
        return new AccountStateImplV2(this);
    }

    @Override
    public void commit() {
        synchronized (updates) {
            if (prev == null) {
                for (Entry<ByteArray, byte[]> entry : updates.entrySet()) {
                    if (entry.getValue() == null) {
                        batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                                .add(BatchOperation.delete(entry.getKey().getData()));
                    } else {
                        batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                                .add(BatchOperation.put(entry.getKey().getData(), entry.getValue()));
                    }
                }

                updateAccountIndex();
            } else {
                for (Entry<ByteArray, byte[]> e : updates.entrySet()) {
                    prev.updates.put(e.getKey(), e.getValue());
                }

                prev.pendingAccounts.addAll(pendingAccounts);
            }

            updates.clear();
            pendingAccounts.clear();
        }
    }

    private void updateAccountIndex() {
        if (pendingAccounts.size() > 0) {
            BigInteger accountCount = getAccountCount();
            BigInteger i = BigInteger.ZERO;
            List<ByteArray> sortedPendingAccounts = pendingAccounts.stream().sorted().collect(Collectors.toList());
            for (ByteArray address : sortedPendingAccounts) {
                BigInteger index = accountCount.add(i);
                i = i.add(BigInteger.ONE);
                batchManager.getBatchInstance(BatchName.ADD_BLOCK).add(
                        BatchOperation.put(getAccountAddressToIndexKey(address.getData()).getData(), Bytes.of(index)));
                batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                        .add(BatchOperation.put(getAccountIndexToAddressKey(index).getData(), address.getData()));

                logger.debug("Update account index {} => {}", index, Hex.encode0x(address.getData()));
            }

            // update account count
            BigInteger updatedAccountCount = accountCount.add(i);
            batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                    .add(BatchOperation.put(getAccountCountKey().getData(), Bytes.of(updatedAccountCount)));

            logger.debug("Update account count: {}", updatedAccountCount);
        }
    }

    @Override
    public BigInteger getAccountIndex(byte[] accountAddress) {
        assert (accountAddress != null && accountAddress.length == 20);
        return Bytes.toBigInteger(database.get(getAccountAddressToIndexKey(accountAddress).getData()));
    }

    @Override
    public BigInteger getAccountCount() {
        return Bytes.toBigInteger(database.get(getAccountCountKey().getData()));
    }

    @Override
    public AccountV2 getAccountByIndex(BigInteger index) {
        assert (index != null && index.compareTo(BigInteger.ZERO) >= 0);
        byte[] address = database.get(getAccountIndexToAddressKey(index).getData());
        return (AccountV2) getAccount(address);
    }

    @Override
    public void rollback() {
        updates.clear();
    }

    @Override
    public boolean exists(byte[] address) {
        ByteArray k = getAddressToBinaryKey(address);

        if (updates.containsKey(k)) {
            return true;
        } else if (prev != null) {
            return prev.exists(address);
        } else {
            byte[] v = database.get(k.getData());
            return v != null;
        }
    }

    @Override
    public long setNonce(byte[] address, long nonce) {
        ByteArray k = getAddressToBinaryKey(address);

        Account acc = getAccount(address);
        acc.setNonce(nonce);
        updates.put(k, new AccountEncoderV2().encode(acc));
        return nonce;
    }

    @Override
    public AccountState clone() {
        AccountStateImplV2 clone = new AccountStateImplV2(database, batchManager);
        clone.prev = prev;
        clone.updates.putAll(updates);

        return clone;
    }

    protected ByteArray getAddressToBinaryKey(byte[] address) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_ACCOUNT, address));
    }

    protected ByteArray getAddressToCodeKey(byte[] address) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_CODE, address));
    }

    protected ByteArray getAddressToStorageKey(byte[] address, byte[] key) {
        byte[] buf = new byte[1 + address.length + key.length];
        buf[0] = DatabasePrefixesV2.TYPE_STORAGE;
        System.arraycopy(address, 0, buf, 1, address.length);
        System.arraycopy(key, 0, buf, 1 + address.length, key.length);

        return ByteArray.of(buf);
    }

    private ByteArray getAccountAddressToIndexKey(byte[] accountAddress) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_ACCOUNT_ADDRESS_TO_INDEX, accountAddress));
    }

    private ByteArray getAccountIndexToAddressKey(BigInteger index) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_ACCOUNT_INDEX_TO_ADDRESS, Bytes.of(index)));
    }

    private ByteArray getAccountCountKey() {
        return ByteArray.of(Bytes.of(DatabasePrefixesV2.TYPE_DELEGATE_COUNT));
    }
}
