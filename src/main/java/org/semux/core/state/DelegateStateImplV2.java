/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.ZERO;
import static org.semux.core.Amount.sub;
import static org.semux.core.Amount.sum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.semux.core.Amount;
import org.semux.core.Blockchain;
import org.semux.crypto.Key;
import org.semux.db.BatchManager;
import org.semux.db.BatchName;
import org.semux.db.BatchOperation;
import org.semux.db.Database;
import org.semux.db.DatabasePrefixesV2;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.ClosableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * DelegateV2 state implementation.
 *
 * <pre>
 * delegate DB structure:
 *
 * [name] => [address] // NOTE: assuming name_length != address_length
 * [address] => [delegate_object]
 * </pre>
 *
 * <pre>
 * vote DB structure:
 *
 * [delegate, voter] => vote
 * </pre>
 *
 */
public class DelegateStateImplV2 implements DelegateStateV2 {

    protected static final Logger logger = LoggerFactory.getLogger(DelegateStateImplV2.class);

    private static final int ADDRESS_LEN = 20;

    protected final Blockchain chain;

    private final Database database;
    private final BatchManager batchManager;
    protected final DelegateStateImplV2 prev;

    private final Cache<ByteArray, Integer> delegateIndexCache = Caffeine.newBuilder().maximumSize(4 * 1000).build();

    /**
     * DelegateV2 updates
     */
    protected final Map<ByteArray, byte[]> delegateUpdates = new ConcurrentHashMap<>();

    final Set<Delegate> pendingDelegates = new ConcurrentSkipListSet<>();

    /**
     * Vote updates
     */
    protected final Map<ByteArray, byte[]> voteUpdates = new ConcurrentHashMap<>();

    /**
     * Create a DelegateState that work directly on a database.
     */
    public DelegateStateImplV2(Blockchain chain, Database database, BatchManager batchManager) {
        this.chain = chain;
        this.database = database;
        this.batchManager = batchManager;
        this.prev = null;
    }

    /**
     * Create an DelegateState based on a previous DelegateState.
     *
     * @param prev
     */
    public DelegateStateImplV2(DelegateStateImplV2 prev) {
        this.chain = prev.chain;
        this.database = prev.database;
        this.batchManager = prev.batchManager;
        this.prev = prev;
    }

    @Override
    public boolean register(byte[] Abyte, byte[] name, long registeredAt) {
        byte[] address = Key.Address.fromAbyte(Abyte);
        if (getDelegateByAddress(address) != null || getDelegateByName(name) != null) {
            return false;
        } else {
            DelegateV2 d = new DelegateV2(Abyte, name, registeredAt, ZERO);
            delegateUpdates.put(getDelegateNameToAddressKey(name), address);
            delegateUpdates.put(getDelegateAddressToBinaryKey(address), new DelegateEncoderV2().encode(d));
            pendingDelegates.add(d);
            return true;
        }
    }

    @Override
    public boolean register(byte[] Abyte, byte[] name) {
        return register(Abyte, name, chain.getLatestBlockNumber() + 1);
    }

    @Override
    public boolean vote(byte[] voter, byte[] delegate, Amount v) {
        Amount value = getVote(voter, delegate);
        Delegate d = getDelegateByAddress(delegate);

        if (d == null) {
            return false;
        } else {
            voteUpdates.put(getVoterAddressToVotesKey(voter, delegate), encodeAmount(sum(value, v)));
            d.setVotes(sum(d.getVotes(), v));
            delegateUpdates.put(getDelegateAddressToBinaryKey(delegate), new DelegateEncoderV2().encode(d));
            return true;
        }
    }

    @Override
    public boolean unvote(byte[] voter, byte[] delegate, Amount v) {
        Amount value = getVote(voter, delegate);

        if (v.gt(value)) {
            return false;
        } else {
            voteUpdates.put(getVoterAddressToVotesKey(voter, delegate), encodeAmount(sub(value, v)));

            Delegate d = getDelegateByAddress(delegate);
            d.setVotes(sub(d.getVotes(), v));
            delegateUpdates.put(getDelegateAddressToBinaryKey(delegate), new DelegateEncoderV2().encode(d));

            return true;
        }
    }

    @Override
    public Amount getVote(byte[] voter, byte[] delegate) {
        ByteArray key = getVoterAddressToVotesKey(voter, delegate);

        if (voteUpdates.containsKey(key)) {
            byte[] bytes = voteUpdates.get(key);
            return decodeAmount(bytes);
        }

        if (prev != null) {
            return prev.getVote(voter, delegate);
        } else {
            byte[] bytes = database.get(key.getData());
            return decodeAmount(bytes);
        }
    }

    @Override
    public Delegate getDelegateByName(byte[] name) {
        ByteArray k = getDelegateNameToAddressKey(name);

        if (delegateUpdates.containsKey(k)) {
            byte[] v = delegateUpdates.get(k);
            return v == null ? null : getDelegateByAddress(v);
        } else if (prev != null) {
            return prev.getDelegateByName(name);
        } else {
            byte[] v = database.get(k.getData());
            return v == null ? null : getDelegateByAddress(v);
        }
    }

    @Override
    public Delegate getDelegateByAddress(byte[] address) {
        ByteArray k = getDelegateAddressToBinaryKey(address);

        if (delegateUpdates.containsKey(k)) {
            byte[] v = delegateUpdates.get(k);
            return v == null ? null : new DelegateDecoderV2().decode(address, v);
        } else if (prev != null) {
            return prev.getDelegateByAddress(address);
        } else {
            byte[] v = database.get(k.getData());
            return v == null ? null : new DelegateDecoderV2().decode(address, v);
        }
    }

    @Override
    public List<Delegate> getDelegates() {
        long t1 = System.nanoTime();

        // traverse all cached update, all the way to database.
        Map<ByteArray, Delegate> map = new HashMap<>();
        getDelegates(map);

        // sort the results
        List<Delegate> list = new ArrayList<>(map.values());
        list.sort((d1, d2) -> {
            int cmp = d2.getVotes().compareTo(d1.getVotes());
            return (cmp != 0) ? cmp : d1.getNameString().compareTo(d2.getNameString());
        });

        long t2 = System.nanoTime();
        logger.trace("Get delegates duration: {} μs", (t2 - t1) / 1000L);
        return list;
    }

    @Override
    public DelegateState track() {
        return new DelegateStateImplV2(this);
    }

    @Override
    public int getDelegateCount() {
        byte[] bytes = database.get(getDelegateCountKey().getData());
        return bytes == null ? 0 : Bytes.toInt(bytes);
    }

    @Override
    public int getDelegateIndex(byte[] delegateAddress) {
        return delegateIndexCache.get(ByteArray.of(delegateAddress),
                (k) -> Bytes.toInt(database.get(getDelegateAddressToIndexKey(k.getData()).getData())));
    }

    @Override
    public Delegate getDelegateByIndex(int index) {
        assert (index >= 0);
        byte[] address = database.get(getDelegateIndexToAddressKey(index).getData());
        return getDelegateByAddress(address);
    }

    private void updateDelegateIndex() {
        if (pendingDelegates.size() > 0) {
            final int delegateCount = getDelegateCount();
            final AtomicInteger i = new AtomicInteger(0);
            pendingDelegates.stream().sorted().forEach((d) -> {
                int index = delegateCount + i.getAndIncrement();
                batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                        .add(BatchOperation
                                .put(getDelegateAddressToIndexKey(d.getAddress()).getData(), Bytes.of(index)));
                batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                        .add(BatchOperation.put(getDelegateIndexToAddressKey(index).getData(), d.getAddress()));

                logger.info("Update delegate index {} => {}", index, d.getAddressString());
            });

            // update delegate count
            final int updatedDelegateCount = delegateCount + i.get();
            batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                    .add(BatchOperation.put(getDelegateCountKey().getData(), Bytes.of(updatedDelegateCount)));

            logger.info("Delegate count: {}", updatedDelegateCount);

            pendingDelegates.clear();
        }
    }

    @Override
    public void commit() {
        synchronized (delegateUpdates) {
            if (prev == null) {
                for (Entry<ByteArray, byte[]> entry : delegateUpdates.entrySet()) {
                    if (entry.getValue() == null) {
                        batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                                .add(BatchOperation.delete(entry.getKey().getData()));
                    } else {
                        batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                                .add(BatchOperation.put(entry.getKey().getData(), entry.getValue()));
                    }
                }

                updateDelegateIndex();
            } else {
                for (Entry<ByteArray, byte[]> e : delegateUpdates.entrySet()) {
                    prev.delegateUpdates.put(e.getKey(), e.getValue());
                }

                prev.pendingDelegates.addAll(pendingDelegates);
            }

            delegateUpdates.clear();
        }

        synchronized (voteUpdates) {
            if (prev == null) {
                for (Entry<ByteArray, byte[]> entry : voteUpdates.entrySet()) {
                    if (entry.getValue() == null) {
                        batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                                .add(BatchOperation.delete(entry.getKey().getData()));
                    } else {
                        batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                                .add(BatchOperation.put(entry.getKey().getData(), entry.getValue()));
                    }
                }
            } else {
                for (Entry<ByteArray, byte[]> e : voteUpdates.entrySet()) {
                    prev.voteUpdates.put(e.getKey(), e.getValue());
                }
            }

            voteUpdates.clear();
        }
    }

    @Override
    public void rollback() {
        delegateUpdates.clear();
        voteUpdates.clear();
    }

    /**
     * Recursively compute the delegates.
     *
     * @param map
     */
    protected void getDelegates(Map<ByteArray, Delegate> map) {
        for (Entry<ByteArray, byte[]> entry : delegateUpdates.entrySet()) {
            /* filter address */
            ByteArray k = ByteArray.of(entry.getKey().getData());
            if (k.length() == ADDRESS_LEN + 1) {
                ByteArray delegateAddress = ByteArray.of(Arrays.copyOfRange(k.getData(), 1, k.length()));
                if (map.containsKey(delegateAddress)) {
                    continue;
                }

                if (entry.getValue() == null) {
                    map.put(delegateAddress, null);
                } else {
                    map.put(delegateAddress,
                            new DelegateDecoderV2().decode(delegateAddress.getData(), entry.getValue()));
                }
            }
        }

        if (prev != null) {
            prev.getDelegates(map);
        } else {
            ClosableIterator<Entry<byte[], byte[]>> itr = database
                    .iterator(Bytes.of(DatabasePrefixesV2.TYPE_DELEGATE));
            while (itr.hasNext()) {
                Entry<byte[], byte[]> entry = itr.next();
                ByteArray k = ByteArray.of(entry.getKey());
                if (k.length() != ADDRESS_LEN + 1 || k.getData()[0] != DatabasePrefixesV2.TYPE_DELEGATE) {
                    continue;
                }

                byte[] v = entry.getValue();
                ByteArray delegateAddress = ByteArray.of(Arrays.copyOfRange(k.getData(), 1, k.length()));
                if (map.containsKey(delegateAddress)) {
                    continue;
                }

                Delegate delegate = new DelegateDecoderV2().decode(delegateAddress.getData(), v);
                map.put(delegateAddress, delegate);
            }
            itr.close();
        }
    }

    @Override
    public Map<ByteArray, Amount> getVotes(byte[] delegate) {
        Map<ByteArray, Amount> result = new HashMap<>();

        ClosableIterator<Entry<byte[], byte[]>> itr = database
                .iterator(Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE_VOTE, delegate));
        while (itr.hasNext()) {
            Entry<byte[], byte[]> e = itr.next();

            if (e.getKey().length != 41) {
                continue;
            }

            byte[] d = Arrays.copyOfRange(e.getKey(), 1, 21);
            byte[] v = Arrays.copyOfRange(e.getKey(), 21, 41);

            if (!Arrays.equals(delegate, d)) {
                break;
            } else if (Bytes.toLong(e.getValue()) != 0) {
                result.put(ByteArray.of(v), decodeAmount(e.getValue()));
            }
        }
        itr.close();

        return result;
    }

    protected byte[] encodeAmount(Amount a) {
        return Bytes.of(a.getNano());
    }

    protected Amount decodeAmount(byte[] bs) {
        return bs == null ? ZERO : NANO_SEM.of(Bytes.toLong(bs));
    }

    // mapping of [delegate name] => [address]
    private ByteArray getDelegateNameToAddressKey(byte[] delegateName) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE, delegateName));
    }

    // mapping of [delegate address] => [delegate binary]
    private ByteArray getDelegateAddressToBinaryKey(byte[] delegateAddress) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE, delegateAddress));
    }

    private ByteArray getDelegateAddressToIndexKey(byte[] delegateAddress) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE_INDEX_TO_ADDRESS, delegateAddress));
    }

    private ByteArray getDelegateIndexToAddressKey(int index) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE_ADDRESS_TO_INDEX, Bytes.of(index)));
    }

    private ByteArray getDelegateCountKey() {
        return ByteArray.of(Bytes.of(DatabasePrefixesV2.TYPE_DELEGATE_COUNT));
    }

    // mapping of [delegate address + voter address] => [votes]
    private ByteArray getVoterAddressToVotesKey(byte[] voterAddress, byte[] delegateAddress) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE_VOTE, Bytes.merge(
                delegateAddress, voterAddress)));
    }

}
