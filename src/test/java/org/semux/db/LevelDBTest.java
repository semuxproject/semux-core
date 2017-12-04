/**
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.util.Bytes;

public class LevelDBTest {

    private byte[] key = Bytes.of("key");
    private byte[] value = Bytes.of("value");

    private KVDB kvdb;

    @Before
    public void setup() {
        kvdb = new LevelDB(new File(Constants.DEFAULT_DATA_DIR, Constants.DATABASE_DIR + File.separator + "test"));
    }

    @After
    public void teardown() {
        kvdb.close();
    }

    @Test
    public void testGetAndPut() {
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
        kvdb.close();

        kvdb.get(key);
    }

    @Test
    public void testDestroy() {
        kvdb.destroy();

        File f = new File(Constants.DEFAULT_DATA_DIR, Constants.DATABASE_DIR + File.separator + "test");
        assertFalse(f.exists());
    }
}
