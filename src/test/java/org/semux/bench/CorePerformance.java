package org.semux.bench;

import java.util.ArrayList;
import java.util.List;

import org.semux.Config;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.EdDSA;
import org.semux.utils.Bytes;
import org.semux.utils.MerkleTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorePerformance {
    private static Logger logger = LoggerFactory.getLogger(CryptoPerformance.class);

    public static Block testBlockCreation() {
        EdDSA key = new EdDSA();

        long t1 = System.nanoTime();

        List<Transaction> txs = new ArrayList<>();
        for (int i = 0; i < Config.MAX_BLOCK_SIZE; i++) {
            TransactionType type = TransactionType.TRANSFER;
            byte[] from = key.toAddress();
            byte[] to = Bytes.random(20);
            long value = 1;
            long fee = Config.MIN_DELEGATE_FEE;
            long nonce = 1 + i;
            long timestamp = System.currentTimeMillis();
            byte[] data = {};
            Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
            tx.sign(key);

            txs.add(tx);
        }

        long number = 1;
        byte[] coinbase = key.toAddress();
        byte[] prevHash = Bytes.random(32);
        long timestamp = System.currentTimeMillis();
        List<byte[]> list = new ArrayList<>();
        for (Transaction tx : txs) {
            list.add(tx.getHash());
        }
        byte[] merkleRoot = new MerkleTree(list).getRootHash();
        byte[] data = {};
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, merkleRoot, data);
        Block block = new Block(header.sign(key), txs);

        long t2 = System.nanoTime();
        logger.info("block size: {} KB", block.toBytes().length / 1024);
        logger.info("Perf_block_creation: {} ms", (t2 - t1) / 1_000_000);
        return block;
    }

    public static void testBlockValidation(Block block) {
        long t1 = System.nanoTime();
        logger.info("validity: {}", block.validate());
        // proof validation is not counted here
        long t2 = System.nanoTime();
        logger.info("Perf_block_validation: {} ms", (t2 - t1) / 1_000_000);
    }

    public static void main(String[] args) throws Exception {
        Block block = testBlockCreation();
        testBlockValidation(block);
    }

}
