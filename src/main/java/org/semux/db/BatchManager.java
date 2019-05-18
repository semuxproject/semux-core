/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import org.semux.util.exception.UnreachableException;

public abstract class BatchManager {

    public abstract Batch getBatchInstance(BatchName batchName);

    public abstract void commit(Batch batch);

    public static BatchManager getInstance(Database database) {
        if (database instanceof LeveldbDatabase) {
            return new LeveldbBatchManager((LeveldbDatabase) database);
        }
        throw new UnreachableException();
    }
}
