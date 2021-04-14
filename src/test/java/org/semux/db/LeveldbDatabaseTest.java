/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.db.LeveldbDatabase.LeveldbFactory;
import org.semux.util.Bytes;
import org.semux.util.ClosableIterator;

public class LeveldbDatabaseTest {

    private byte[] key = Bytes.of("key");
    private byte[] value = Bytes.of("value");

    private LeveldbDatabase db;

    @Before
    public void setup() throws IOException {
        Path temp = Files.createTempDirectory("db");
        db = new LeveldbDatabase(temp.toFile());
    }

    @After
    public void teardown() {
        db.destroy();
    }

    @Test
    public void testRecover() {
        db.recover(db.createOptions());
    }

    @Test
    public void testGetAndPut() {
        assertNull(db.get(key));
        db.put(key, value);
        assertArrayEquals(value, db.get(key));
    }

    @Test
    public void testUpdateBatch() {
        db.put(Bytes.of("a"), Bytes.of("1"));

        List<Pair<byte[], byte[]>> update = new ArrayList<>();
        update.add(Pair.of(Bytes.of("a"), null));
        update.add(Pair.of(Bytes.of("b"), Bytes.of("2")));
        update.add(Pair.of(Bytes.of("c"), Bytes.of("3")));
        db.updateBatch(update);

        assertNull(db.get(Bytes.of("a")));
        assertArrayEquals(db.get(Bytes.of("b")), Bytes.of("2"));
        assertArrayEquals(db.get(Bytes.of("c")), Bytes.of("3"));
    }

    @Test
    public void testIterator() {
        db.put(Bytes.of("a"), Bytes.of("1"));
        db.put(Bytes.of("b"), Bytes.of("2"));
        db.put(Bytes.of("c"), Bytes.of("3"));

        ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator(Bytes.of("a1"));
        assertTrue(itr.hasNext());
        assertArrayEquals(Bytes.of("b"), itr.next().getKey());
        assertTrue(itr.hasNext());
        assertArrayEquals(Bytes.of("c"), itr.next().getKey());
        itr.close();
    }

    @Test
    public void testClose() {
        db.close();
    }

    @Test
    public void testDestroy() {
        db.destroy();

        assertFalse(db.getDataDir().toFile().exists());
    }
}
