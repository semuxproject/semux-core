/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.util.ClosableIterator;

public interface KVDB {

    /**
     * Get the value that is mapped to the specified key.
     * 
     * @param key
     * @return
     */
    public byte[] get(byte[] key);

    /**
     * Associate a value to the specified key.
     * 
     * 
     * 
     * @param key
     * @param value
     *            can not be null
     */
    public void put(byte[] key, byte[] value);

    /**
     * Delete the specified key value pair if present.
     * 
     * 
     * @param key
     */
    public void delete(byte[] key);

    /**
     * Update a list of key value pairs.
     * 
     * @param pairs
     *            key value pairs; pair with null value, will be deleted
     */
    public void updateBatch(List<Pair<byte[], byte[]>> pairs);

    /**
     * Get all the keys. NOTE: be sure to close it after iteration.
     * 
     * @return
     */
    public ClosableIterator<Entry<byte[], byte[]>> iterator();

    /**
     * Get all the keys. NOTE: be sure to close it after iteration.
     * 
     * @return
     */
    public ClosableIterator<Entry<byte[], byte[]>> iterator(byte[] prefix);

    /**
     * Close the database.
     */
    public void close();

    /**
     * Destroy this DB.
     * 
     */
    public void destory();
}
