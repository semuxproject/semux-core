/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.nio.file.Path;

import org.semux.config.Config;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.BlockchainImplV2;
import org.semux.core.Genesis;
import org.semux.core.event.BlockchainDatabaseUpgradingEvent;
import org.semux.db.exception.MigrationException;
import org.semux.event.PubSub;
import org.semux.event.PubSubFactory;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database migration from version 1 to version 2. This migration unifies all
 * LevelDB databases defined in ${@link DatabaseName} into a single database
 * which is ${@link DatabaseName#BLOCK}. The migration deprecates
 * ${@link BlockchainImpl}, ${@link org.semux.core.state.AccountStateImpl},
 * ${@link org.semux.core.state.DelegateStateImpl} in favor of
 * ${@link org.semux.core.BlockchainImplV2},
 * ${@link org.semux.core.state.AccountStateImplV2},
 * ${@link org.semux.core.state.DelegateStateImplV2}.
 */
public class MigrationBlockDbVersion002 implements Migration {
    private static final Logger logger = LoggerFactory.getLogger(MigrationBlockDbVersion002.class);

    private final PubSub pubSub = PubSubFactory.getDefault();

    private final Genesis genesis;
    private final BlockchainImpl blockchain;

    public MigrationBlockDbVersion002(Genesis genesis, BlockchainImpl blockchain) {
        this.genesis = genesis;
        this.blockchain = blockchain;
    }

    @Override
    public void checkBlockchainVersion(Blockchain blockchain) throws MigrationException {
        if (!(blockchain instanceof BlockchainImpl)) {
            throw new MigrationException("Unsupported blockchain version");
        }

        int version = ((BlockchainImpl) blockchain).getDatabaseVersion();
        if (version != 1 && version != 0) {
            throw new MigrationException("Unsupported database version");
        }
    }

    @Override
    public void migrate(Config config, DatabaseFactory dbFactory) throws MigrationException {
        try {
            logger.info("Upgrading the database... DO NOT CLOSE THE WALLET!");
            // recreate block db in a temporary folder
            String dbName = dbFactory.getDataDir().getFileName().toString();
            Path tempPath = dbFactory
                    .getDataDir()
                    .resolveSibling(dbName + "_tmp_" + TimeUtil.currentTimeMillis());
            LeveldbDatabase.LeveldbFactory tempDbFactory = new LeveldbDatabase.LeveldbFactory(tempPath.toFile());
            Database tempDb = tempDbFactory.getDB(DatabaseName.BLOCK);
            BlockchainImplV2.MigrationBlockchain migrationBlockchain = new BlockchainImplV2.MigrationBlockchain(config,
                    genesis, tempDb, BatchManager.getInstance(tempDb));
            final long latestBlockNumber = blockchain.getLatestBlockNumber();
            for (long i = 1; i <= latestBlockNumber; i++) {
                migrationBlockchain.applyBlock(blockchain.getBlock(i));
                if (i % 1000 == 0) {
                    pubSub.publish(new BlockchainDatabaseUpgradingEvent(i, latestBlockNumber));
                    logger.info("Loaded {} / {} blocks", i, latestBlockNumber);
                }
            }
            dbFactory.close();
            tempDbFactory.close();
            // move the existing database to backup folder then replace the database folder
            // with the upgraded database
            Path backupPath = dbFactory
                    .getDataDir()
                    .resolveSibling(
                            dbFactory.getDataDir().getFileName().toString() + "_bak_"
                                    + TimeUtil.currentTimeMillis());
            dbFactory.moveTo(backupPath);
            tempDbFactory.moveTo(dbFactory.getDataDir());
            dbFactory.open();
            logger.info("Database upgraded to version 2.");
        } catch (Exception e) {
            logger.error("Failed to run migration " + MigrationBlockDbVersion002.class, e);
            throw new MigrationException(e);
        }
    }
}
