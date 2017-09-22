/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.semux.Config;
import org.semux.utils.ClosableIterator;
import org.semux.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LevelDB implements KVDB {

    private static final Logger logger = LoggerFactory.getLogger(LevelDB.class);

    private static final String DATABASE_DIR = "database";

    private DBName name;
    private DB db;

    public LevelDB(DBName name) {
        this.name = name;

        Options options = new Options();
        options.createIfMissing(true);
        options.cacheSize(128 * 1024 * 1024);
        options.compressionType(CompressionType.NONE);

        try {
            File f = getFile(name);
            f.getParentFile().mkdirs();

            db = JniDBFactory.factory.open(f, options);
        } catch (IOException e) {
            logger.error("Failed to open database", e);
            System.exit(-1);
        }
    }

    @Override
    public byte[] get(byte[] key) {
        return db.get(key);
    }

    @Override
    public void put(byte[] key, byte[] value) {
        db.put(key, value);
    }

    @Override
    public void delete(byte[] key) {
        db.delete(key);
    }

    @Override
    public void updateBatch(List<Pair<byte[], byte[]>> pairs) {
        WriteBatch batch = db.createWriteBatch();
        try {
            for (Pair<byte[], byte[]> p : pairs) {
                if (p.getValue() == null) {
                    batch.delete(p.getLeft());
                } else {
                    batch.put(p.getLeft(), p.getRight());
                }
            }
            db.write(batch);
        } finally {
            try {
                batch.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        try {
            db.close();
        } catch (IOException e) {
            logger.error("Failed to close database: {}", name, e);
        }
    }

    @Override
    public void destory() {
        close();
        FileUtil.recursiveDelete(getFile(name));
    }

    @Override
    public ClosableIterator<Entry<byte[], byte[]>> iterator() {

        return new ClosableIterator<Entry<byte[], byte[]>>() {
            DBIterator itr = db.iterator();
            {
                itr.seekToFirst();
            }

            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }

            @Override
            public Entry<byte[], byte[]> next() {
                return itr.next();
            }

            @Override
            public void close() {
                try {
                    itr.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private File getFile(DBName name) {
        return new File(Config.DATA_DIR, DATABASE_DIR + File.separator + name.toString().toLowerCase());
    }
}
