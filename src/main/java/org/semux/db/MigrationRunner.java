/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import org.semux.config.Config;
import org.semux.core.Blockchain;
import org.semux.db.exception.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);

    private final Config config;

    private final DatabaseFactory databaseFactory;

    private final Blockchain blockchain;

    public MigrationRunner(Config config, DatabaseFactory databaseFactory, Blockchain blockchain) {
        this.config = config;
        this.databaseFactory = databaseFactory;
        this.blockchain = blockchain;
    }

    public void migrate(Migration migration) throws MigrationException {
        migration.checkBlockchainVersion(blockchain);
        logger.info("Running migration {}...", migration.getClass().getSimpleName());
        migration.migrate(config, databaseFactory);
    }
}