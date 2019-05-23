/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import static org.semux.core.Amount.Unit.NANO_SEM;

import java.util.ArrayList;
import java.util.List;

import org.semux.Network;
import org.semux.TestUtils;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.BlockEncoder;
import org.semux.core.BlockEncoderV1;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainFactory;
import org.semux.core.Genesis;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Key;
import org.semux.crypto.Key.Signature;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockchainPerformance {
    private static final Logger logger = LoggerFactory.getLogger(BlockchainPerformance.class);

    private static Config config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);
    private static Key key = new Key();

    public static Block testBlockCreation() {
        long t1 = System.nanoTime();

        List<Transaction> txs = new ArrayList<>();
        List<TransactionResult> res = new ArrayList<>();

        int total = 0;
        for (int i = 0;; i++) {
            Network network = config.network();
            TransactionType type = TransactionType.TRANSFER;
            byte[] to = Bytes.random(20);
            Amount value = NANO_SEM.of(1);
            Amount fee = config.minTransactionFee();
            long nonce = 1 + i;
            long timestamp = TimeUtil.currentTimeMillis();
            byte[] data = Bytes.EMPTY_BYTES;
            Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(key);

            if (total + tx.size() > config.maxBlockTransactionsSize()) {
                break;
            }

            txs.add(tx);
            res.add(new TransactionResult());
            total += tx.size();
        }

        long number = 1;
        byte[] coinbase = key.toAddress();
        byte[] prevHash = Bytes.random(32);
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, txs, res);

        List<Signature> votes = new ArrayList<>();
        for (int i = 0; i < config.getNumberOfValidators(1000000L); i++) {
            votes.add(new Key().sign(Bytes.EMPTY_BYTES));
        }
        block.setView(1);
        block.setVotes(votes);

        BlockEncoder blockEncoder = new BlockEncoderV1();
        long t2 = System.nanoTime();
        logger.info("block # of txs: {}", block.getTransactions().size());
        logger.info("block header size: {} B", blockEncoder.encoderHeader(block).length);
        logger.info("block transaction size: {} KB", blockEncoder.encodeTransactions(block).length / 1024);
        logger.info("block results size: {} KB", blockEncoder.encodeTransactionResults(block).length / 1024);
        logger.info("block votes size: {} KB", blockEncoder.encodeVotes(block).length / 1024);
        logger.info("block total size: {} KB", blockEncoder.encode(block).length / 1024);
        logger.info("Perf_block_creation: {} ms", (t2 - t1) / 1_000_000);
        return block;
    }

    public static void testBlockValidation(Block block) {
        Genesis gen = Genesis.load(Network.DEVNET);

        long t1 = System.nanoTime();
        block.validateHeader(gen.getHeader(), block.getHeader());
        block.validateTransactions(gen.getHeader(), block.getTransactions(), config.network());
        block.validateResults(gen.getHeader(), block.getResults());
        // block votes validation skipped
        long t2 = System.nanoTime();

        logger.info("Perf_block_validation: {} ms", (t2 - t1) / 1_000_000);
    }

    public static void testTransactionValidation() {
        Key key = new Key();

        Network network = config.network();
        TransactionType type = TransactionType.TRANSFER;
        byte[] to = Bytes.random(20);
        Amount value = NANO_SEM.of(1);
        Amount fee = config.minTransactionFee();
        long nonce = 1;
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = {};
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        int repeat = 1000;
        long t1 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            tx.validate(network);
        }
        long t2 = System.nanoTime();
        logger.info("Perf_transaction_size: {} B", tx.toBytes().length);
        logger.info("Perf_transaction_validation: {} μs/time", (t2 - t1) / repeat / 1_000);
    }

    public static void testAddBlock() throws Throwable {
        final int repeat = 10000;
        Block[] blocks = new Block[repeat];
        for (int i = 0; i < repeat; i++) {
            blocks[i] = TestUtils.createEmptyBlock(i);
        }

        TemporaryDatabaseRule temporaryDbRule = new TemporaryDatabaseRule();
        temporaryDbRule.before();
        Blockchain blockchain = new BlockchainFactory(config, Genesis.load(Network.DEVNET), temporaryDbRule)
                .getBlockchainInstance();
        long t1 = TimeUtil.currentTimeMillis();
        for (int i = 0; i < repeat; i++) {
            blockchain.addBlock(blocks[i]);
        }
        long t2 = TimeUtil.currentTimeMillis();
        temporaryDbRule.after();
        logger.info("Perf_addBlock: {} ms / {} blocks", t2 - t1, repeat);
    }

    public static void main(String[] args) throws Throwable {
        Block block = testBlockCreation();
        testBlockValidation(block);
        testTransactionValidation();
        testAddBlock();

        System.exit(0);
    }
}
