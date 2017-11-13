/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.iq80.leveldb.DBException;
import org.junit.Test;
import org.semux.Config;
import org.semux.util.Bytes;

public class LevelDBTest {

    private byte[] key = Bytes.of("key");
    private byte[] value = Bytes.of("value");

    @Test
    public void testGetAndPut() {
        KVDB kvdb = new LevelDB(DBName.BLOCK);

        try {
            assertNull(kvdb.get(key));
            kvdb.put(key, value);
            assertTrue(Arrays.equals(value, kvdb.get(key)));
            kvdb.delete(key);
        } finally {
            kvdb.close();
        }
    }

    @Test(expected = DBException.class)
    public void testClose() {
        KVDB kvdb = new LevelDB(DBName.BLOCK);
        kvdb.close();

        kvdb.get(key);
    }

    @Test
    public void testDestroy() {
        KVDB kvdb = new LevelDB(DBName.BLOCK);
        kvdb.destory();

        File dir = new File(Config.DATA_DIR, "database");
        File f = new File(dir, DBName.BLOCK.toString().toLowerCase());
        assertFalse(f.exists());
    }
}
