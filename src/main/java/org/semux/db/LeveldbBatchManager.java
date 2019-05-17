/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.iq80.leveldb.WriteBatch;
import org.semux.db.exception.DatabaseException;
import org.semux.util.SystemUtil;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeveldbBatchManager implements BatchManager {

    private static final Logger logger = LoggerFactory.getLogger(LeveldbBatchManager.class);

    private LeveldbDatabase leveldbDatabase;

    private ConcurrentHashMap<BatchName, Batch> batchInstances = new ConcurrentHashMap<>();

    public LeveldbBatchManager(LeveldbDatabase leveldbDatabase) {
        this.leveldbDatabase = leveldbDatabase;
    }

    @Override
    public Batch getBatchInstance(BatchName batchName) {
        return batchInstances.computeIfAbsent(batchName, Batch::new);
    }

    @Override
    public void commit(Batch batch) {
        if (!batch.setCommitted()) {
            throw new DatabaseException("batch re-committed");
        }

        try (WriteBatch writeBatch = leveldbDatabase.createWriteBatch()) {
            batch.stream().forEach((batchOperation) -> {
                switch (batchOperation.type) {
                case PUT:
                    writeBatch.put(batchOperation.key, batchOperation.value);
                    break;
                case DELETE:
                    writeBatch.delete(batchOperation.key);
                    break;
                default:
                    throw new UnreachableException();
                }
            });
            leveldbDatabase.writeBatch(writeBatch);
            batchInstances.remove(batch.name);
        } catch (IOException e) {
            logger.error("Failed to update batch", e);
            SystemUtil.exitAsync(SystemUtil.Code.FAILED_TO_WRITE_BATCH_TO_DB);
        }
    }
}
