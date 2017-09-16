package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.utils.Bytes;

public class BlockTest {
    private long number = 1;
    private byte[] coinbase = Bytes.random(20);
    private byte[] prevHash = Bytes.random(32);
    private long timestamp = System.currentTimeMillis();
    private byte[] transactionsRoot = Bytes.random(32);
    private byte[] resultsRoot = Bytes.random(32);
    private byte[] stateRoot = Bytes.random(32);
    private byte[] data = Bytes.of("data");

    private Transaction tx = new Transaction(TransactionType.TRANSFER, Bytes.random(20), Bytes.random(20), 0,
            Config.MIN_TRANSACTION_FEE, 1, System.currentTimeMillis(), Bytes.EMPY_BYTES).sign(new EdDSA());
    private TransactionResult res = new TransactionResult(true);
    private List<Transaction> transactions = Collections.singletonList(tx);
    private List<TransactionResult> results = Collections.singletonList(res);
    private int view = 1;
    private List<Signature> votes = new ArrayList<>();

    private EdDSA key = new EdDSA();
    private byte[] hash;
    private byte[] signature;

    @Test
    public void testNew() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data).sign(key);
        Block block = new Block(header, transactions, results, view, votes);
        hash = block.getHash();
        signature = block.getSignature().toBytes();

        testFields(block);
    }

    @Test
    public void testSerilization() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data).sign(key);
        Block block = new Block(header, transactions, results, view, votes);
        hash = block.getHash();
        signature = block.getSignature().toBytes();

        testFields(Block.fromBytes(block.toBytes()));
    }

    private void testFields(Block block) {
        assertArrayEquals(hash, block.getHash());
        assertEquals(number, block.getNumber());
        assertArrayEquals(coinbase, block.getCoinbase());
        assertArrayEquals(prevHash, block.getPrevHash());
        assertEquals(timestamp, block.getTimestamp());
        assertArrayEquals(transactionsRoot, block.getTransactionsRoot());
        assertArrayEquals(resultsRoot, block.getResultsRoot());
        assertArrayEquals(stateRoot, block.getStateRoot());
        assertArrayEquals(data, block.getData());
        assertArrayEquals(signature, block.getSignature().toBytes());
        assertEquals(view, block.getView());
        assertTrue(block.getVotes().isEmpty());
    }

    @Test
    public void testTransactionIndexes() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data).sign(key);
        Block block = new Block(header, transactions, results, view, votes);

        List<Pair<Integer, Integer>> indexes = block.getTransacitonIndexes();
        assertEquals(1, indexes.size());

        byte[] bytes = block.toBytes();
        Pair<Integer, Integer> index = indexes.get(0);
        Transaction tx2 = Transaction
                .fromBytes(Arrays.copyOfRange(bytes, index.getLeft(), index.getLeft() + index.getRight()));
        assertArrayEquals(tx.getHash(), tx2.getHash());
    }
}
