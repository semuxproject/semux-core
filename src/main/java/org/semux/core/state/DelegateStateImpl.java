/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.semux.core.Blockchain;
import org.semux.db.KVDB;
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
 * [name] => [address] // NOTE: assuming name_length != adress_length
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
public class DelegateStateImpl implements DelegateState {

    protected static final Logger logger = LoggerFactory.getLogger(DelegateStateImpl.class);

    private static final int ADDRESS_LEN = 20;

    protected Blockchain chain;
    protected KVDB delegateDB;
    protected KVDB voteDB;
    protected DelegateStateImpl prev;

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
    public DelegateStateImpl(Blockchain chain, KVDB delegateDB, KVDB voteDB) {
        this.chain = chain;
        this.delegateDB = delegateDB;
        this.voteDB = voteDB;
    }

    /**
     * Create an DelegateState based on a previous DelegateState.
     * 
     * @param prev
     */
    public DelegateStateImpl(DelegateStateImpl prev) {
        this.chain = prev.chain;
        this.prev = prev;
    }

    @Override
    public boolean register(byte[] address, byte[] name, long registeredAt) {
        if (getDelegateByAddress(address) != null || getDelegateByName(name) != null) {
            return false;
        } else {
            Delegate d = new Delegate(address, name, registeredAt, 0);
            delegateUpdates.put(ByteArray.of(name), address);
            delegateUpdates.put(ByteArray.of(address), d.toBytes());

            return true;
        }
    }

    @Override
    public boolean register(byte[] address, byte[] name) {
        return register(address, name, chain.getLatestBlockNumber() + 1);
    }

    @Override
    public boolean vote(byte[] voter, byte[] delegate, long v) {
        ByteArray key = ByteArray.of(Bytes.merge(delegate, voter));
        long value = getVote(key);
        Delegate d = getDelegateByAddress(delegate);

        if (d == null) {
            return false;
        } else {
            voteUpdates.put(key, Bytes.of(value + v));
            d.setVotes(d.getVotes() + v);
            delegateUpdates.put(ByteArray.of(delegate), d.toBytes());
            return true;
        }
    }

    @Override
    public boolean unvote(byte[] voter, byte[] delegate, long v) {
        ByteArray key = ByteArray.of(Bytes.merge(delegate, voter));
        long value = getVote(key);

        if (v > value) {
            return false;
        } else {
            voteUpdates.put(key, Bytes.of(value - v));

            Delegate d = getDelegateByAddress(delegate);
            d.setVotes(d.getVotes() - v);
            delegateUpdates.put(ByteArray.of(delegate), d.toBytes());

            return true;
        }
    }

    @Override
    public long getVote(byte[] voter, byte[] delegate) {
        return getVote(ByteArray.of(Bytes.merge(delegate, voter)));
    }

    @Override
    public Delegate getDelegateByName(byte[] name) {
        ByteArray k = ByteArray.of(name);

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
    public Delegate getDelegateByAddress(byte[] addr) {
        ByteArray k = ByteArray.of(addr);

        if (delegateUpdates.containsKey(k)) {
            byte[] v = delegateUpdates.get(k);
            return v == null ? null : Delegate.fromBytes(k.getData(), v);
        } else if (prev != null) {
            return prev.getDelegateByAddress(addr);
        } else {
            byte[] v = delegateDB.get(k.getData());
            return v == null ? null : Delegate.fromBytes(k.getData(), v);
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
            int cmp = Long.compare(d2.getVotes(), d1.getVotes());
            return (cmp != 0) ? cmp : d1.getNameString().compareTo(d2.getNameString());
        });

        long t2 = System.nanoTime();
        logger.trace("Get delegates duration: {} μs", (t2 - t1) / 1000L);
        return list;
    }

    @Override
    public DelegateState track() {
        return new DelegateStateImpl(this);
    }

    @Override
    public void commit() {
        synchronized (delegateUpdates) {
            if (prev == null) {
                for (ByteArray k : delegateUpdates.keySet()) {
                    byte[] v = delegateUpdates.get(k);
                    if (v == null) {
                        delegateDB.delete(k.getData());
                    } else {
                        delegateDB.put(k.getData(), v);
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
                for (ByteArray k : voteUpdates.keySet()) {
                    byte[] v = voteUpdates.get(k);
                    if (v == null) {
                        voteDB.delete(k.getData());
                    } else {
                        voteDB.put(k.getData(), v);
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
        for (ByteArray k : delegateUpdates.keySet()) {
            /* filter address */
            if (k.length() == ADDRESS_LEN && !map.containsKey(k)) {
                byte[] v = delegateUpdates.get(k);

                if (v == null) {
                    map.put(k, null);
                } else {
                    map.put(k, Delegate.fromBytes(k.getData(), v));
                }
            }
        }

        if (prev != null) {
            prev.getDelegates(map);
        } else {
            ClosableIterator<Entry<byte[], byte[]>> itr = delegateDB.iterator();
            while (itr.hasNext()) {
                Entry<byte[], byte[]> entry = itr.next();
                ByteArray k = ByteArray.of(entry.getKey());
                byte[] v = entry.getValue();

                if (k.length() == ADDRESS_LEN && !map.containsKey(k)) {
                    map.put(k, Delegate.fromBytes(k.getData(), v));
                }
            }
            itr.close();
        }
    }

    /**
     * Get the vote that one voter has given to the specified delegate.
     * 
     * @param key
     *            the byte array representation of [delegate, voter].
     * @return
     */
    protected long getVote(ByteArray key) {
        if (voteUpdates.containsKey(key)) {
            byte[] bytes = voteUpdates.get(key);
            return (bytes == null) ? 0 : Bytes.toLong(bytes);
        }

        if (prev != null) {
            return prev.getVote(key);
        } else {
            byte[] bytes = voteDB.get(key.getData());
            return (bytes == null) ? 0 : Bytes.toLong(bytes);
        }
    }

    @Override
    public Map<ByteArray, Long> getVotes(byte[] delegate) {
        Map<ByteArray, Long> result = new HashMap<>();

        ClosableIterator<Entry<byte[], byte[]>> itr = voteDB.iterator(delegate);
        while (itr.hasNext()) {
            Entry<byte[], byte[]> e = itr.next();
            byte[] d = Arrays.copyOf(e.getKey(), 20);
            byte[] v = Arrays.copyOfRange(e.getKey(), 20, 40);

            if (!Arrays.equals(delegate, d)) {
                break;
            } else if (Bytes.toLong(e.getValue()) != 0) {
                result.put(ByteArray.of(v), Bytes.toLong(e.getValue()));
            }
        }
        itr.close();

        return result;
    }
}
