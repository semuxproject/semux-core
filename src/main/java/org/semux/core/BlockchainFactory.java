/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.Map;

import org.semux.config.Config;
import org.semux.core.exception.BlockchainException;
import org.semux.db.BatchManager;
import org.semux.db.Database;
import org.semux.db.DatabaseFactory;
import org.semux.db.DatabaseName;
import org.semux.db.DatabasePrefixesV1;
import org.semux.db.DatabasePrefixesV2;
import org.semux.db.DatabaseVersion;
import org.semux.db.LeveldbBatchManager;
import org.semux.db.LeveldbDatabase;
import org.semux.db.MigrationBlockDbVersion001;
import org.semux.db.MigrationBlockDbVersion002;
import org.semux.db.MigrationRunner;
import org.semux.util.Bytes;
import org.semux.util.ClosableIterator;

public class BlockchainFactory {

    Config config;

    Genesis genesis;

    DatabaseFactory databaseFactory;

    public BlockchainFactory(Config config, Genesis genesis, DatabaseFactory databaseFactory) {
        this.config = config;
        this.genesis = genesis;
        this.databaseFactory = databaseFactory;
    }

    /**
     * Gets the latest blockchain instance that is either upgraded from an existing
     * older version of blockchain database or created from the genesis block. The
     * factory will try to perform a rolling upgrade from v0 to the latest version.
     *
     * @return the latest version of blockchain implementation
     */
    public Blockchain getBlockchainInstance() {
        final DatabaseVersion databaseVersion = getBlockchainDatabaseVersion();
        if (needsInitialization()) {
            new BlockchainImpl(config, genesis, databaseFactory);
            return getBlockchainInstance();
        } else if (databaseVersion == DatabaseVersion.V0) {
            BlockchainImpl blockchain = new BlockchainImpl(config, genesis, databaseFactory);
            MigrationRunner migrationRunner = new MigrationRunner(config, databaseFactory, blockchain);
            migrationRunner.migrate(new MigrationBlockDbVersion001(blockchain));
            return getBlockchainInstance();
        } else if (databaseVersion == DatabaseVersion.V1) {
            BlockchainImpl blockchain = new BlockchainImpl(config, genesis, databaseFactory);
            MigrationRunner migrationRunner = new MigrationRunner(config, databaseFactory, blockchain);
            migrationRunner.migrate(new MigrationBlockDbVersion002());
            return getBlockchainInstance();
        } else if (databaseVersion == DatabaseVersion.V2) {
            Database database = databaseFactory.getDB(DatabaseName.BLOCK);
            return new BlockchainImplV2(
                    config,
                    genesis,
                    database,
                    BatchManager.getInstance(database));
        } else {
            throw new BlockchainException("Unsupported blockchain database version " + databaseVersion);
        }
    }

    public DatabaseVersion getBlockchainDatabaseVersion() {
        byte[] versionBytes;

        // check v2 version bytes
        if (databaseFactory.exists(DatabaseName.BLOCK)) {
            Database blockDB = databaseFactory.getDB(DatabaseName.BLOCK);
            versionBytes = blockDB.get(Bytes.of(DatabasePrefixesV2.TYPE_DATABASE_VERSION));
            if (versionBytes != null && versionBytes.length > 0) {
                return DatabaseVersion.fromBytes(versionBytes);
            }
        }

        // check v1 version bytes
        if (databaseFactory.exists(DatabaseName.INDEX)) {
            Database indexDB = databaseFactory.getDB(DatabaseName.INDEX);
            versionBytes = indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_DATABASE_VERSION));
            if (versionBytes != null && versionBytes.length > 0) {
                return DatabaseVersion.fromBytes(versionBytes);
            }
        }

        return DatabaseVersion.V0;
    }

    protected boolean needsInitialization() {
        Database blockDb = databaseFactory.getDB(DatabaseName.BLOCK);
        ClosableIterator<Map.Entry<byte[], byte[]>> iterator = blockDb
                .iterator(Bytes.of(DatabasePrefixesV2.TYPE_BLOCK_HEADER));
        boolean result = !iterator.hasNext();
        iterator.close();
        return result;
    }
}
