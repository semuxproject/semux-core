/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.TestnetConfig;
import org.semux.core.state.Delegate;
import org.semux.crypto.Hex;
import org.semux.db.DatabaseFactory;
import org.semux.db.DatabaseVersion;
import org.semux.db.LeveldbDatabase;
import org.semux.net.filter.SemuxIpFilterLoaderTest;

public class BlockchainImplMigrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testMigrateBlockDBFromVersion0() throws IOException {
        testMigration("/database/database-v0.tgz");
    }

    @Test
    public void testMigrateBlockDbFromVersion1() throws IOException {
        testMigration("/database/database-v1.tgz");
    }

    private void testMigration(String tarball) throws IOException {
        // extract a database from resource bundle
        File dbVersion0Tarball = new File(
                SemuxIpFilterLoaderTest.class.getResource(tarball).getFile());
        Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
        archiver.extract(dbVersion0Tarball, temporaryFolder.getRoot());

        // load the database
        DatabaseFactory dbFactory = new LeveldbDatabase.LeveldbFactory(new File(temporaryFolder.getRoot(), "database"));
        Config config = new TestnetConfig(Constants.DEFAULT_DATA_DIR);
        BlockchainFactory blockchainFactory = new BlockchainFactory(config, Genesis.load(Network.TESTNET), dbFactory);
        Blockchain blockchain = blockchainFactory.getBlockchainInstance();

        assertBlockchain(blockchainFactory, blockchain);
    }

    private void assertBlockchain(BlockchainFactory blockchainFactory, Blockchain blockchain) {
        assertThat("getDatabaseVersion", blockchainFactory.getBlockchainDatabaseVersion(), equalTo(DatabaseVersion.V2));
        assertThat("getLatestBlockNumber", blockchain.getLatestBlockNumber(), equalTo(29L));
        for (int i = 0; i <= blockchain.getLatestBlockNumber(); i++) {
            assertThat("getBlock(" + i + ")", blockchain.getBlock(i), is(notNullValue()));
        }

        byte[] address = Hex.decode("8e1ef4f810a84bea2fbf1e5f4afd8d0774ab0038");
        assertThat("getAccount(8e1ef4f810a84bea2fbf1e5f4afd8d0774ab0038)",
                blockchain.getAccountState().getAccount(address).getAvailable(),
                equalTo(Amount.Unit.NANO_SEM.of(10000000000000000L)));

        List<Delegate> delegates = blockchain.getDelegateState().getDelegates();
        assertThat("delegates.size()", delegates.size(), equalTo(4));

        // blockchain.getDelegateState().getDelegateByAddress();
    }

}
