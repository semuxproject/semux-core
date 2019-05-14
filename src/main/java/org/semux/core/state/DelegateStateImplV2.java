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
import java.util.concurrent.ConcurrentHashMap;

import org.semux.core.Amount;
import org.semux.core.Blockchain;
import org.semux.db.Database;
import org.semux.db.DatabasePrefixesV2;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.ClosableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegate state implementation.
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
public class DelegateStateImplV2 implements DelegateState {

    protected static final Logger logger = LoggerFactory.getLogger(DelegateStateImplV2.class);

    private static final int ADDRESS_LEN = 20;

    protected final Blockchain chain;

    protected Database delegateDB;
    protected Database voteDB;
    protected DelegateStateImplV2 prev;

    /**
     * Delegate updates
     */
    protected final Map<ByteArray, byte[]> delegateUpdates = new ConcurrentHashMap<>();

    /**
     * Vote updates
     */
    protected final Map<ByteArray, byte[]> voteUpdates = new ConcurrentHashMap<>();

    /**
     * Create a DelegateState that work directly on a database.
     *
     * @param delegateDB
     * @param voteDB
     */
    public DelegateStateImplV2(Blockchain chain, Database delegateDB, Database voteDB) {
        this.chain = chain;
        this.delegateDB = delegateDB;
        this.voteDB = voteDB;
    }

    /**
     * Create an DelegateState based on a previous DelegateState.
     *
     * @param prev
     */
    public DelegateStateImplV2(DelegateStateImplV2 prev) {
        this.chain = prev.chain;
        this.prev = prev;
    }

    @Override
    public boolean register(byte[] address, byte[] name, long registeredAt) {
        if (getDelegateByAddress(address) != null || getDelegateByName(name) != null) {
            return false;
        } else {
            Delegate d = new Delegate(address, name, registeredAt, ZERO);
            delegateUpdates.put(getDelegateNameToAddressKey(name), address);
            delegateUpdates.put(getDelegateAddressToBinaryKey(address), d.toBytes());

            return true;
        }
    }

    @Override
    public boolean register(byte[] address, byte[] name) {
        return register(address, name, chain.getLatestBlockNumber() + 1);
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
            delegateUpdates.put(getDelegateAddressToBinaryKey(delegate), d.toBytes());
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
            delegateUpdates.put(getDelegateAddressToBinaryKey(delegate), d.toBytes());

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
            byte[] bytes = voteDB.get(key.getData());
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
            byte[] v = delegateDB.get(k.getData());
            return v == null ? null : getDelegateByAddress(v);
        }
    }

    @Override
    public Delegate getDelegateByAddress(byte[] address) {
        ByteArray k = getDelegateAddressToBinaryKey(address);

        if (delegateUpdates.containsKey(k)) {
            byte[] v = delegateUpdates.get(k);
            return v == null ? null : Delegate.fromBytes(address, v);
        } else if (prev != null) {
            return prev.getDelegateByAddress(address);
        } else {
            byte[] v = delegateDB.get(k.getData());
            return v == null ? null : Delegate.fromBytes(address, v);
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
    public void commit() {
        synchronized (delegateUpdates) {
            if (prev == null) {
                for (Entry<ByteArray, byte[]> entry : delegateUpdates.entrySet()) {
                    if (entry.getValue() == null) {
                        delegateDB.delete(entry.getKey().getData());
                    } else {
                        delegateDB.put(entry.getKey().getData(), entry.getValue());
                    }
                }
            } else {
                for (Entry<ByteArray, byte[]> e : delegateUpdates.entrySet()) {
                    prev.delegateUpdates.put(e.getKey(), e.getValue());
                }
            }

            delegateUpdates.clear();
        }

        synchronized (voteUpdates) {
            if (prev == null) {
                for (Entry<ByteArray, byte[]> entry : voteUpdates.entrySet()) {
                    if (entry.getValue() == null) {
                        voteDB.delete(entry.getKey().getData());
                    } else {
                        voteDB.put(entry.getKey().getData(), entry.getValue());
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
            if (k.length() == ADDRESS_LEN + 1 && !map.containsKey(k)) {
                ByteArray delegateAddress = ByteArray.of(Arrays.copyOfRange(k.getData(), 1, k.length()));

                if (entry.getValue() == null) {
                    map.put(delegateAddress, null);
                } else {
                    map.put(delegateAddress, Delegate.fromBytes(delegateAddress.getData(), entry.getValue()));
                }
            }
        }

        if (prev != null) {
            prev.getDelegates(map);
        } else {
            ClosableIterator<Entry<byte[], byte[]>> itr = delegateDB
                    .iterator(Bytes.of(DatabasePrefixesV2.TYPE_DELEGATE));
            while (itr.hasNext()) {
                Entry<byte[], byte[]> entry = itr.next();
                ByteArray k = ByteArray.of(entry.getKey());
                byte[] v = entry.getValue();

                if (k.length() == ADDRESS_LEN + 1 && !map.containsKey(k)) {
                    byte[] delegateAddress = Arrays.copyOfRange(k.getData(), 1, k.length());
                    map.put(ByteArray.of(delegateAddress), Delegate.fromBytes(delegateAddress, v));
                }
            }
            itr.close();
        }
    }

    @Override
    public Map<ByteArray, Amount> getVotes(byte[] delegate) {
        Map<ByteArray, Amount> result = new HashMap<>();

        ClosableIterator<Entry<byte[], byte[]>> itr = voteDB
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

    // mapping of [delegate address + voter address] => [votes]
    private ByteArray getVoterAddressToVotesKey(byte[] voterAddress, byte[] delegateAddress) {
        return ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE_VOTE, Bytes.merge(
                delegateAddress, voterAddress)));
    }

}
