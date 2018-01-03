/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.util.ArrayList;
import java.util.List;

import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Genesis;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockchainPerformance {
    private static final Logger logger = LoggerFactory.getLogger(BlockchainPerformance.class);

    private static Config config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);
    private static EdDSA key = new EdDSA();

    public static Block testBlockCreation() {
        long t1 = System.nanoTime();

        List<Transaction> txs = new ArrayList<>();
        List<TransactionResult> res = new ArrayList<>();

        int total = 0;
        for (int i = 0;; i++) {
            byte networkId = config.networkId();
            TransactionType type = TransactionType.TRANSFER;
            byte[] to = Bytes.random(20);
            long value = 1;
            long fee = config.minTransactionFee();
            long nonce = 1 + i;
            long timestamp = System.currentTimeMillis();
            byte[] data = Bytes.EMPTY_BYTES;
            Transaction tx = new Transaction(networkId, type, to, value, fee, nonce, timestamp, data).sign(key);

            if (total + tx.size() > config.maxBlockTransactionsSize()) {
                break;
            }

            txs.add(tx);
            res.add(new TransactionResult(true));
            total += tx.size();
        }

        long number = 1;
        byte[] coinbase = key.toAddress();
        byte[] prevHash = Bytes.random(32);
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, txs, res);

        List<Signature> votes = new ArrayList<>();
        for (int i = 0; i < config.getNumberOfValidators(1000000L); i++) {
            votes.add(new EdDSA().sign(Bytes.EMPTY_BYTES));
        }
        block.setView(1);
        block.setVotes(votes);

        long t2 = System.nanoTime();
        logger.info("block # of txs: {}", block.getTransactions().size());
        logger.info("block header size: {} B", block.toBytesHeader().length);
        logger.info("block transaction size: {} KB", block.toBytesTransactions().length / 1024);
        logger.info("block results size: {} KB", block.toBytesResults().length / 1024);
        logger.info("block votes size: {} KB", block.toBytesVotes().length / 1024);
        logger.info("block total size: {} KB", block.size() / 1024);
        logger.info("Perf_block_creation: {} ms", (t2 - t1) / 1_000_000);
        return block;
    }

    public static void testBlockValidation(Block block) {
        Genesis gen = Genesis.load(config.dataDir());

        long t1 = System.nanoTime();
        Block.validateHeader(gen.getHeader(), block.getHeader());
        Block.validateTransactions(gen.getHeader(), block.getTransactions(), config.networkId());
        Block.validateResults(gen.getHeader(), block.getResults());
        // block votes validation skipped
        long t2 = System.nanoTime();

        logger.info("Perf_block_validation: {} ms", (t2 - t1) / 1_000_000);
    }

    public static void testTransactionValidation() {
        EdDSA key = new EdDSA();

        byte networkId = config.networkId();
        TransactionType type = TransactionType.TRANSFER;
        byte[] to = Bytes.random(20);
        long value = 1;
        long fee = config.minTransactionFee();
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = {};
        Transaction tx = new Transaction(networkId, type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        int repeat = 1000;
        long t1 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            tx.validate(networkId);
        }
        long t2 = System.nanoTime();
        logger.info("Perf_transaction_size: {} B", tx.toBytes().length);
        logger.info("Perf_transaction_validation: {} μs/time", (t2 - t1) / repeat / 1_000);
    }

    public static void main(String[] args) throws Exception {
        Block block = testBlockCreation();
        testBlockValidation(block);
        testTransactionValidation();

        System.exit(0);
    }
}
