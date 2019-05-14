/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.config.Config;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.db.exception.MigrationException;
import org.semux.event.PubSub;
import org.semux.event.PubSubFactory;
import org.semux.util.Bytes;
import org.semux.util.ClosableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationBlockDbVersion002 implements Migration {
    private static final Logger logger = LoggerFactory.getLogger(MigrationBlockDbVersion001.class);

    private final PubSub pubSub = PubSubFactory.getDefault();

    @Override
    public void checkBlockchainVersion(Blockchain blockchain) throws MigrationException {
        if (!(blockchain instanceof BlockchainImpl)) {
            throw new MigrationException("Unsupported blockchain version");
        }

        if (((BlockchainImpl) blockchain).getDatabaseVersion() != 1) {
            throw new MigrationException("Unsupported database version");
        }
    }

    @Override
    public void migrate(Config config, DatabaseFactory dbFactory) {
        try {
            List<Pair<byte[], byte[]>> batch = new ArrayList<>();

            // move indexDB
            Database indexDB = dbFactory.getDB(DatabaseName.INDEX);
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_LATEST_BLOCK_NUMBER),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_LATEST_BLOCK_NUMBER))));
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_VALIDATORS),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_VALIDATORS))));
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_VALIDATOR_STATS),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_VALIDATOR_STATS))));
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_BLOCK_HASH),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_BLOCK_HASH))));
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_TRANSACTION_HASH),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_TRANSACTION_HASH))));
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_ACCOUNT_TRANSACTION),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_ACCOUNT_TRANSACTION))));
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_ACTIVATED_FORKS),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_ACTIVATED_FORKS))));
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_COINBASE_TRANSACTION_HASH),
                    indexDB.get(Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_COINBASE_TRANSACTION_HASH))));

            // move accountDB
            Database accountDB = dbFactory.getDB(DatabaseName.ACCOUNT);
            ClosableIterator<Map.Entry<byte[], byte[]>> accountDBIt = accountDB
                    .iterator(Bytes.of(DatabasePrefixesV1.AccountDB.TYPE_ACCOUNT));
            accountDBIt.forEachRemaining((Map.Entry<byte[], byte[]> accountEntry) -> batch.add(Pair.of(
                    Bytes.merge(DatabasePrefixesV2.TYPE_ACCOUNT,
                            Arrays.copyOfRange(accountEntry.getKey(), 1, accountEntry.getKey().length)),
                    accountEntry.getValue())));
            accountDBIt.close();
            accountDBIt = accountDB.iterator(Bytes.of(DatabasePrefixesV1.AccountDB.TYPE_CODE));
            accountDBIt.forEachRemaining((Map.Entry<byte[], byte[]> accountEntry) -> batch.add(Pair.of(
                    Bytes.merge(DatabasePrefixesV2.TYPE_CODE,
                            Arrays.copyOfRange(accountEntry.getKey(), 1, accountEntry.getKey().length)),
                    accountEntry.getValue())));
            accountDBIt.close();
            accountDBIt = accountDB.iterator(Bytes.of(DatabasePrefixesV1.AccountDB.TYPE_STORAGE));
            accountDBIt.forEachRemaining((Map.Entry<byte[], byte[]> accountEntry) -> batch.add(Pair.of(
                    Bytes.merge(DatabasePrefixesV2.TYPE_STORAGE,
                            Arrays.copyOfRange(accountEntry.getKey(), 1, accountEntry.getKey().length)),
                    accountEntry.getValue())));
            accountDBIt.close();
            accountDB.close();

            // move delegateDB
            Database delegateDB = dbFactory.getDB(DatabaseName.DELEGATE);
            ClosableIterator<Map.Entry<byte[], byte[]>> delegateDBIt = delegateDB.iterator();
            delegateDBIt.forEachRemaining((Map.Entry<byte[], byte[]> delegateEntry) -> batch.add(Pair.of(
                    Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE, delegateEntry.getKey()),
                    delegateEntry.getValue())));
            delegateDBIt.close();
            delegateDB.close();

            // move voteDB
            Database voteDB = dbFactory.getDB(DatabaseName.VOTE);
            ClosableIterator<Map.Entry<byte[], byte[]>> voteDBIt = voteDB.iterator();
            voteDBIt.forEachRemaining((Map.Entry<byte[], byte[]> voteEntry) -> batch.add(Pair.of(
                    Bytes.merge(DatabasePrefixesV2.TYPE_DELEGATE_VOTE, voteEntry.getKey()),
                    voteEntry.getValue())));
            voteDBIt.close();
            voteDB.close();

            // upgrade version byte
            batch.add(Pair.of(
                    Bytes.of(DatabasePrefixesV2.TYPE_DATABASE_VERSION),
                    Bytes.of(DatabaseVersion.V2.toInt())));

            // run batch
            Database blockDB = dbFactory.getDB(DatabaseName.BLOCK);
            blockDB.updateBatch(batch);

            // mark v1 db as upgraded
            indexDB.put(
                    Bytes.of(DatabasePrefixesV1.IndexDB.TYPE_DATABASE_VERSION),
                    Bytes.of(DatabaseVersion.V2.toInt()));
            indexDB.close();

            logger.info("Database upgraded to version 2.");
        } catch (Exception e) {
            logger.error("Failed to run migration " + MigrationBlockDbVersion002.class, e);
        }
    }
}
