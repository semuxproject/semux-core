/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.db.LeveldbDatabase.LeveldbFactory;
import org.semux.db.exception.DatabaseException;
import org.semux.util.Bytes;
import org.semux.util.ClosableIterator;

public class LeveldbDatabaseTest {

    private byte[] key = Bytes.of("key");
    private byte[] value = Bytes.of("value");

    public LeveldbDatabase openDatabase() {
        return new LeveldbDatabase(
                new File(Constants.DEFAULT_DATA_DIR, Constants.DATABASE_DIR + File.separator + "test"));
    }

    @Test
    public void testRecover() {
        LeveldbDatabase db = openDatabase();
        try {
            db.recover(db.createOptions());
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testGetAndPut() {
        LeveldbDatabase db = openDatabase();
        try {
            assertNull(db.get(key));
            db.put(key, value);
            assertTrue(Arrays.equals(value, db.get(key)));
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testUpdateBatch() {
        LeveldbDatabase db = openDatabase();
        try {
            db.put(Bytes.of("a"), Bytes.of("1"));

            List<Pair<byte[], byte[]>> update = new ArrayList<>();
            update.add(Pair.of(Bytes.of("a"), null));
            update.add(Pair.of(Bytes.of("b"), Bytes.of("2")));
            update.add(Pair.of(Bytes.of("c"), Bytes.of("3")));
            db.updateBatch(update);

            assertNull(db.get(Bytes.of("a")));
            assertArrayEquals(db.get(Bytes.of("b")), Bytes.of("2"));
            assertArrayEquals(db.get(Bytes.of("c")), Bytes.of("3"));
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testBatchManager() {
        LeveldbDatabase db = openDatabase();
        try {
            BatchManager batchManager = new LeveldbBatchManager(db);
            Batch batch = batchManager.getBatchInstance(BatchName.ADD_BLOCK);
            batch.add(
                    BatchOperation.put(Bytes.of("a"), Bytes.of("1")),
                    BatchOperation.delete(Bytes.of("a")),
                    BatchOperation.put(Bytes.of("b"), Bytes.of("2")),
                    BatchOperation.put(Bytes.of("c"), Bytes.of("3")));
            batchManager.commit(batch);

            assertNull(db.get(Bytes.of("a")));
            assertArrayEquals(db.get(Bytes.of("b")), Bytes.of("2"));
            assertArrayEquals(db.get(Bytes.of("c")), Bytes.of("3"));
            assertThat(batchManager.getBatchInstance(BatchName.ADD_BLOCK).stream().count(), equalTo(0L));
        } finally {
            db.destroy();
        }
    }

    @Test(expected = DatabaseException.class)
    public void testBatchManagerRecommit() {
        LeveldbDatabase db = openDatabase();
        try {
            BatchManager batchManager = new LeveldbBatchManager(db);
            Batch batch = batchManager.getBatchInstance(BatchName.ADD_BLOCK);
            batchManager.commit(batch);
            batchManager.commit(batch);
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testIterator() {
        LeveldbDatabase db = openDatabase();
        try {
            db.put(Bytes.of("a"), Bytes.of("1"));
            db.put(Bytes.of("b"), Bytes.of("2"));
            db.put(Bytes.of("c"), Bytes.of("3"));

            ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator(Bytes.of("a1"));
            assertTrue(itr.hasNext());
            assertArrayEquals(Bytes.of("b"), itr.next().getKey());
            assertTrue(itr.hasNext());
            assertArrayEquals(Bytes.of("c"), itr.next().getKey());
            itr.close();
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testLevelDBFactory() {
        LeveldbFactory factory = new LeveldbFactory(new File(Constants.DEFAULT_DATA_DIR, Constants.DATABASE_DIR));
        for (DatabaseName name : DatabaseName.values()) {
            assertNotNull(factory.getDB(name));
        }
        factory.close();

        // NOTE: empty databases are created
    }

    @Test
    public void testClose() {
        LeveldbDatabase db = openDatabase();
        try {
            db.close();
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testDestroy() {
        LeveldbDatabase db = openDatabase();
        db.destroy();

        assertFalse(db.getDataDir().toFile().exists());
    }
}
