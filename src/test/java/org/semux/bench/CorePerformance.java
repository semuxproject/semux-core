package org.semux.bench;

import java.util.ArrayList;
import java.util.List;

import org.semux.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.crypto.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorePerformance {
    private static Logger logger = LoggerFactory.getLogger(CryptoPerformance.class);

    public static Block testBlockCreation() {
        EdDSA key = new EdDSA();

        long t1 = System.nanoTime();

        List<Transaction> txs = new ArrayList<>();
        List<TransactionResult> res = new ArrayList<>();

        for (int i = 0; i < Config.MAX_BLOCK_SIZE; i++) {
            TransactionType type = TransactionType.TRANSFER;
            byte[] from = key.toAddress();
            byte[] to = Bytes.random(20);
            long value = 1;
            long fee = Config.DELEGATE_BURN_AMOUNT;
            long nonce = 1 + i;
            long timestamp = System.currentTimeMillis();
            byte[] data = Bytes.random(128);
            Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
            tx.sign(key);

            txs.add(tx);
            res.add(new TransactionResult(true));
        }

        long number = 1;
        byte[] coinbase = key.toAddress();
        byte[] prevHash = Bytes.random(32);
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
        byte[] stateRoot = Hash.EMPTY_H256;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header.sign(key), txs, res);

        List<Signature> votes = new ArrayList<>();
        for (int i = 0; i < Config.getNumberOfValidators(1000000L); i++) {
            votes.add(new EdDSA().sign(Bytes.EMPY_BYTES));
        }
        block.setView(1);
        block.setVotes(votes);

        long t2 = System.nanoTime();
        logger.info("block header size: {} KB", block.toBytesHeader().length / 1024);
        logger.info("block transaction size: {} KB", block.toBytesTransactions().length / 1024);
        logger.info("block results size: {} KB", block.toBytesResults().length / 1024);
        logger.info("block votes size: {} KB", block.toBytesVotes().length / 1024);
        logger.info("Perf_block_creation: {} ms", (t2 - t1) / 1_000_000);
        return block;
    }

    public static void testBlockValidation(Block block) {
        long t1 = System.nanoTime();
        // proof validation is not counted here
        long t2 = System.nanoTime();
        logger.info("Perf_block_validation: {} ms", (t2 - t1) / 1_000_000);
    }

    public static void testTransactionValidation() {
        EdDSA key = new EdDSA();

        TransactionType type = TransactionType.TRANSFER;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        long value = 1;
        long fee = Config.DELEGATE_BURN_AMOUNT;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = {};
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        int repeat = 1000;
        long t1 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            tx.validate();
        }
        long t2 = System.nanoTime();
        logger.info("Perf_transaction_size: {} B", tx.toBytes().length);
        logger.info("Perf_transaction_validation: {} μs/time", (t2 - t1) / repeat / 1_000);
    }

    public static void main(String[] args) throws Exception {
        Block block = testBlockCreation();
        testBlockValidation(block);
        testTransactionValidation();
    }
}
