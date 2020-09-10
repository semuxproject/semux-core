/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.TestnetConfig;
import org.semux.db.DatabaseFactory;
import org.semux.db.LeveldbDatabase;
import org.semux.net.filter.SemuxIpFilterLoaderTest;

public class BlockchainImplMigrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testMigrationBlockDbVersion001() throws IOException {
        // extract a version 0 database from resource bundle
        File dbVersion0Tarball = new File(
                SemuxIpFilterLoaderTest.class.getResource("/database/database-v0.tgz").getFile());
        Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
        archiver.extract(dbVersion0Tarball, temporaryFolder.getRoot());

        // load the database
        DatabaseFactory dbFactory = new LeveldbDatabase.LeveldbFactory(new File(temporaryFolder.getRoot(), "database"));
        Config config = new TestnetConfig(Constants.DEFAULT_DATA_DIR);
        BlockchainImpl blockchain = new BlockchainImpl(config, dbFactory);

        // the database should be upgraded to the latest version
        assertThat("getDatabaseVersion", blockchain.getDatabaseVersion(), equalTo(BlockchainImpl.DATABASE_VERSION));
        assertThat("getLatestBlockNumber", blockchain.getLatestBlockNumber(), equalTo(29L));
        for (int i = 0; i <= blockchain.getLatestBlockNumber(); i++) {
            assertThat("getBlock(" + i + ")", blockchain.getBlock(i), is(notNullValue()));
        }
    }

}
