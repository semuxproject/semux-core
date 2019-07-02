/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.semux.util.ByteArray;
import org.semux.util.exception.UnreachableException;

public class Batch {

    private ArrayDeque<BatchOperation> operations = new ArrayDeque<>();

    private HashMap<ByteArray, byte[]> pendingState = new HashMap<>();

    public final BatchName name;

    private AtomicBoolean committed = new AtomicBoolean(false);

    public Batch(BatchName name) {
        this.name = name;
    }

    synchronized public void add(BatchOperation batchOperation) {
        operations.add(batchOperation);
        switch (batchOperation.type) {
        case PUT:
            pendingState.put(ByteArray.of(batchOperation.key), batchOperation.value);
            break;
        case DELETE:
            pendingState.put(ByteArray.of(batchOperation.key), null);
            break;
        default:
            throw new UnreachableException();
        }
    }

    synchronized public void add(BatchOperation... batchOperations) {
        for (BatchOperation batchOperation : batchOperations) {
            add(batchOperation);
        }
    }

    synchronized public Stream<BatchOperation> stream() {
        return operations.stream();
    }

    synchronized public void clear() {
        operations.clear();
        pendingState.clear();
    }

    synchronized public boolean containsPendingState(ByteArray key) {
        return pendingState.containsKey(key);
    }

    synchronized public byte[] lookupPendingState(ByteArray key) {
        return pendingState.get(key);
    }

    boolean setCommitted() {
        return committed.compareAndSet(false, true);
    }
}
