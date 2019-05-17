/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Batch {

    private List<BatchOperation> operations = new LinkedList<>();

    public final BatchName name;

    private AtomicBoolean committed = new AtomicBoolean(false);

    public Batch(BatchName name) {
        this.name = name;
    }

    synchronized public void add(BatchOperation batchOperation) {
        operations.add(batchOperation);
    }

    synchronized public Stream<BatchOperation> stream() {
        return operations.stream();
    }

    synchronized public void clear() {
        operations.clear();
    }

    boolean setCommitted() {
        return committed.compareAndSet(false, true);
    }
}
