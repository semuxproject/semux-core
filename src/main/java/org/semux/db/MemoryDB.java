/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.util.ByteArray;
import org.semux.util.ClosableIterator;

public class MemoryDB implements KVDB {

    public static final DBFactory FACTORY = new DBFactory() {
        @Override
        public KVDB getDB(DBName name) {
            return new MemoryDB();
        }
    };

    private Map<ByteArray, byte[]> db = new ConcurrentHashMap<>();

    @Override
    public byte[] get(byte[] key) {
        return db.get(ByteArray.of(key));
    }

    @Override
    public void put(byte[] key, byte[] value) {
        db.put(ByteArray.of(key), value);
    }

    @Override
    public void delete(byte[] key) {
        db.remove(ByteArray.of(key));
    }

    @Override
    public void updateBatch(List<Pair<byte[], byte[]>> pairs) {
        for (Pair<byte[], byte[]> p : pairs) {
            if (p.getValue() == null) {
                delete(p.getLeft());
            } else {
                put(p.getLeft(), p.getRight());
            }
        }
    }

    @Override
    public ClosableIterator<Entry<byte[], byte[]>> iterator() {
        return new ClosableIterator<Entry<byte[], byte[]>>() {

            Iterator<Entry<ByteArray, byte[]>> itr = db.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }

            @Override
            public Entry<byte[], byte[]> next() {
                Entry<ByteArray, byte[]> entry = itr.next();

                return new Entry<byte[], byte[]>() {
                    @Override
                    public byte[] getKey() {
                        return entry.getKey().getData();
                    }

                    @Override
                    public byte[] getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public byte[] setValue(byte[] value) {
                        return entry.getValue();
                    }
                };
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public ClosableIterator<Entry<byte[], byte[]>> iterator(byte[] prefix) {
        throw new UnsupportedOperationException("Key iterator with prefix is not supported");
    }

    @Override
    public void close() {
    }

    @Override
    public void destory() {
    }
}
