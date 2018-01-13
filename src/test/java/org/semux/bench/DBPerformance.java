/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.io.File;

import org.semux.config.Constants;
import org.semux.db.LevelDb;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBPerformance {
    private static final Logger logger = LoggerFactory.getLogger(DBPerformance.class);

    private static final int REPEAT = 100_000;

    private static LevelDb getTestDB() {
        return new LevelDb(new File(Constants.DEFAULT_DATA_DIR, "test"));
    }

    public static void testWrite() {
        LevelDb db = getTestDB();
        try {
            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                byte[] key = Bytes.random(256);
                byte[] value = Bytes.random(256);
                db.put(key, value);
            }
            long t2 = System.nanoTime();
            logger.info("Perf_db_write: " + (t2 - t1) / 1_000 / REPEAT + " μs/time");
        } finally {
            db.close();
        }
    }

    public static void testRead() {
        LevelDb db = getTestDB();
        try {
            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                byte[] key = Bytes.random(256);
                db.get(key);
            }
            long t2 = System.nanoTime();
            logger.info("Perf_db_read: " + (t2 - t1) / 1_000 / REPEAT + " μs/time");
        } finally {
            db.close();
        }
    }

    public static void main(String[] args) {
        testWrite();
        testRead();

        LevelDb db = getTestDB();
        db.destroy();
    }
}
