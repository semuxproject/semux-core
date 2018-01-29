/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.tuple.Pair;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.semux.config.Constants;
import org.semux.db.exception.LevelDbException;
import org.semux.util.ClosableIterator;
import org.semux.util.FileUtil;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LevelDb implements Db {

    private static final Logger logger = LoggerFactory.getLogger(LevelDb.class);

    private File file;
    private DB db;
    private boolean isOpened;

    public LevelDb(File file) {
        this.file = file;

        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            logger.error("Failed to create directory: {}", dir);
        }

        open(createOptions());
    }

    /**
     * Creates the default options.
     *
     * @return
     */
    protected Options createOptions() {
        Options options = new Options();
        options.createIfMissing(true);
        options.compressionType(CompressionType.NONE);
        options.blockSize(4 * 1024 * 1024);
        options.writeBufferSize(8 * 1024 * 1024);
        options.cacheSize(64L * 1024L * 1024L);
        options.paranoidChecks(true);
        options.verifyChecksums(true);
        options.maxOpenFiles(128);

        return options;
    }

    /**
     * Open the database.
     * 
     * @param options
     */
    protected void open(Options options) {
        try {
            db = JniDBFactory.factory.open(file, options);
            isOpened = true;
        } catch (IOException e) {
            if (e.getMessage().contains("Corruption")) {
                // recover
                recover(options);

                // reopen
                try {
                    db = JniDBFactory.factory.open(file, options);
                    isOpened = true;
                } catch (IOException ex) {
                    logger.error("Failed to open database", e);
                    SystemUtil.exitAsync(-1);
                }
            } else {
                logger.error("Failed to open database", e);
                SystemUtil.exitAsync(-1);
            }
        }
    }

    /**
     * Tries to recover the database in case of corruption.
     *
     * @param options
     */
    protected void recover(Options options) {
        try {
            logger.info("Database is corrupted, trying to repair...");
            factory.repair(file, options);
            logger.info("Repair done!");
        } catch (IOException ex) {
            logger.error("Failed to repair the database", ex);
            SystemUtil.exitAsync(-1);
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
        try (WriteBatch batch = db.createWriteBatch()) {
            for (Pair<byte[], byte[]> p : pairs) {
                if (p.getValue() == null) {
                    batch.delete(p.getLeft());
                } else {
                    batch.put(p.getLeft(), p.getRight());
                }
            }
            db.write(batch);
        } catch (IOException e) {
            logger.error("Failed to update batch", e);
            SystemUtil.exitAsync(-1);
        }
    }

    @Override
    public void close() {
        try {
            if (isOpened) {
                db.close();
                isOpened = false;
            }
        } catch (IOException e) {
            logger.error("Failed to close database: {}", file, e);
        }
    }

    @Override
    public void destroy() {
        close();
        FileUtil.recursiveDelete(file);
    }

    @Override
    public ClosableIterator<Entry<byte[], byte[]>> iterator() {
        return iterator(null);
    }

    @Override
    public ClosableIterator<Entry<byte[], byte[]>> iterator(byte[] prefix) {

        return new ClosableIterator<Entry<byte[], byte[]>>() {
            DBIterator itr = db.iterator();

            private ClosableIterator<Entry<byte[], byte[]>> initialize() {
                if (prefix != null) {
                    itr.seek(prefix);
                } else {
                    itr.seekToFirst();
                }
                return this;
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
                    throw new LevelDbException(e);
                }
            }
        }.initialize();
    }

    public static class LevelDbFactory implements DbFactory {

        private EnumMap<DbName, Db> databases = new EnumMap<>(DbName.class);

        private File dataDir;
        private AtomicBoolean open;

        public LevelDbFactory(File dataDir) {
            this.dataDir = dataDir;
            this.open = new AtomicBoolean(false);

            open();
        }

        protected void open() {
            if (open.compareAndSet(false, true)) {
                for (DbName name : DbName.values()) {
                    File file = new File(dataDir,
                            Constants.DATABASE_DIR + File.separator + name.toString().toLowerCase());
                    databases.put(name, new LevelDb(file));
                }
            }
        }

        @Override
        public Db getDB(DbName name) {
            open();
            return databases.get(name);
        }

        @Override
        public void close() {
            if (open.compareAndSet(true, false)) {
                for (Db db : databases.values()) {
                    db.close();
                }
            }
        }
    }
}
