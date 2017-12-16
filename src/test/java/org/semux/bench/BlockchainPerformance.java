/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.EdDSA;
import org.semux.db.DBFactory;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockchainPerformance {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainPerformance.class);

    private static final byte[] coinbase = Bytes.random(30);
    private static final byte[] prevHash = Bytes.random(32);
    private static final EdDSA key = new EdDSA();
    private static final long value = 20;
    private static final long fee = 1;
    private static final long nonce = 12345;
    private static final byte[] data = Bytes.of("test");
    private static final long timestamp = System.currentTimeMillis() - 60 * 1000;

    /**
     * The benchmark tries to create a block filled with TRANSFER_MANY transactions
     * each with Transaction.MAX_RECIPIENTS recipients
     */
    private static void testLargeBlock(DBFactory dbFactory) {
        Instant begin = Instant.now();

        Config config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);
        BlockchainImpl blockchain = new BlockchainImpl(config, dbFactory);

        ArrayList<Transaction> transactions = new ArrayList<>();
        ArrayList<TransactionResult> transactionResults = new ArrayList<>();
        // there can be 50 transactions in this case
        for (int i = 1; i <= config.maxBlockSize() / (Transaction.MAX_RECIPIENTS / 2); i++) {
            Transaction tx = new Transaction(
                    TransactionType.TRANSFER_MANY,
                    Bytes.random(EdDSA.ADDRESS_LEN * Transaction.MAX_RECIPIENTS),
                    value,
                    fee,
                    nonce,
                    timestamp,
                    data).sign(key);
            transactions.add(tx);
            transactionResults.add(new TransactionResult(true));
        }
        Block block = new Block(
                new BlockHeader(
                        1,
                        coinbase,
                        prevHash,
                        timestamp,
                        MerkleUtil.computeTransactionsRoot(transactions),
                        MerkleUtil.computeResultsRoot(transactionResults),
                        Bytes.EMPTY_HASH,
                        Bytes.EMPTY_BYTES),
                transactions,
                transactionResults);
        blockchain.addBlock(block);

        Duration duration = Duration.between(begin, Instant.now());

        logger.info("Block Size = {} bytes, took {}\n", block.size(), TimeUtil.formatDuration(duration));
    }

    public static void main(String[] args) throws Throwable {
        TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();
        temporaryDBFactory.before();
        testLargeBlock(temporaryDBFactory);
        temporaryDBFactory.after();
    }
}
