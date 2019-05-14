/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.io.IOException;
import java.nio.file.Path;

import org.semux.config.Config;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.event.BlockchainDatabaseUpgradingEvent;
import org.semux.db.exception.MigrationException;
import org.semux.event.PubSub;
import org.semux.event.PubSubFactory;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database migration from version 0 to version 1. The migration process creates
 * a temporary ${@link BlockchainImpl.MigrationBlockchain} then migrates all
 * blocks from an existing blockchain database to the created temporary
 * blockchain database. Once all blocks have been successfully migrated, the
 * existing blockchain database is replaced by the migrated temporary blockchain
 * database.
 */
public class MigrationBlockDbVersion001 implements Migration {
    private static final Logger logger = LoggerFactory.getLogger(MigrationBlockDbVersion001.class);

    private final PubSub pubSub = PubSubFactory.getDefault();

    private final BlockchainImpl blockchain;

    public MigrationBlockDbVersion001(BlockchainImpl blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public void checkBlockchainVersion(Blockchain blockchain) throws MigrationException {
        if (!(blockchain instanceof BlockchainImpl)) {
            throw new MigrationException("Unsupported blockchain version");
        }

        if (((BlockchainImpl) blockchain).getDatabaseVersion() != 0) {
            throw new MigrationException("Unsupported database version");
        }
    }

    @Override
    public void migrate(Config config, DatabaseFactory dbFactory) {
        try {
            logger.info("Upgrading the database... DO NOT CLOSE THE WALLET!");
            // recreate block db in a temporary folder
            String dbName = dbFactory.getDataDir().getFileName().toString();
            Path tempPath = dbFactory
                    .getDataDir()
                    .resolveSibling(dbName + "_tmp_" + TimeUtil.currentTimeMillis());
            LeveldbDatabase.LeveldbFactory tempDb = new LeveldbDatabase.LeveldbFactory(tempPath.toFile());
            BlockchainImpl.MigrationBlockchain migrationBlockchain = blockchain.getMigrationBlockchainInstance(config,
                    tempDb);
            final long latestBlockNumber = blockchain.getLatestBlockNumber();
            for (long i = 1; i <= latestBlockNumber; i++) {
                migrationBlockchain.applyBlock(blockchain.getBlock(i));
                if (i % 1000 == 0) {
                    pubSub.publish(new BlockchainDatabaseUpgradingEvent(i, latestBlockNumber));
                    logger.info("Loaded {} / {} blocks", i, latestBlockNumber);
                }
            }
            dbFactory.close();
            tempDb.close();
            // move the existing database to backup folder then replace the database folder
            // with the upgraded database
            Path backupPath = dbFactory
                    .getDataDir()
                    .resolveSibling(
                            dbFactory.getDataDir().getFileName().toString() + "_bak_"
                                    + TimeUtil.currentTimeMillis());
            dbFactory.moveTo(backupPath);
            tempDb.moveTo(dbFactory.getDataDir());
            dbFactory.open();
            logger.info("Database upgraded to version 1.");
        } catch (IOException e) {
            logger.error("Failed to run migration " + MigrationBlockDbVersion001.class, e);
        }
    }

}