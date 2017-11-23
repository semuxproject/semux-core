/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.SimpleDecoder;

public class BlockTest {
    private long number = 5;
    private byte[] coinbase = Bytes.random(20);
    private byte[] prevHash = Bytes.random(32);
    private long timestamp = System.currentTimeMillis();
    private byte[] data = Bytes.of("data");

    private Transaction tx = new Transaction(TransactionType.TRANSFER, Bytes.random(20), 0, Config.MIN_TRANSACTION_FEE,
            1, System.currentTimeMillis(), Bytes.EMPY_BYTES).sign(new EdDSA());
    private TransactionResult res = new TransactionResult(true);
    private List<Transaction> transactions = Collections.singletonList(tx);
    private List<TransactionResult> results = Collections.singletonList(res);
    private int view = 1;
    private List<Signature> votes = new ArrayList<>();

    private byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(transactions);
    private byte[] resultsRoot = MerkleUtil.computeResultsRoot(results);
    private byte[] stateRoot = Bytes.EMPTY_HASH;

    private byte[] hash;

    @Test
    public void testGenesis() {
        Block block = Genesis.getInstance();
        assertTrue(block.getHeader().validate());
    }

    @Test
    public void testNew() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, transactions, results, view, votes);
        hash = block.getHash();

        testFields(block);
    }

    @Test
    public void testSerilization() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, transactions, results, view, votes);
        hash = block.getHash();

        testFields(Block.fromBytes(block.toBytesHeader(), block.toBytesTransactions(), block.toBytesResults(),
                block.toBytesVotes()));
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
        assertEquals(view, block.getView());
        assertTrue(block.getVotes().isEmpty());
    }

    @Test
    public void testTransactionIndexes() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, transactions, results, view, votes);

        List<Pair<Integer, Integer>> indexes = block.getTransacitonIndexes();
        assertEquals(1, indexes.size());

        Pair<Integer, Integer> index = indexes.get(0);
        SimpleDecoder dec = new SimpleDecoder(block.toBytesTransactions(), index.getLeft());
        Transaction tx2 = Transaction.fromBytes(dec.readBytes());
        assertArrayEquals(tx.getHash(), tx2.getHash());
    }

    @Test
    public void testValidateTransactions() {
        BlockHeader previousHeader = new BlockHeader(number - 1, coinbase, prevHash, timestamp - 1, transactionsRoot,
                resultsRoot, stateRoot, data);
        BlockHeader header = new BlockHeader(number, coinbase, previousHeader.getHash(), timestamp, transactionsRoot,
                resultsRoot, stateRoot, data);

        assertTrue(Block.validateHeader(previousHeader, header));
        assertTrue(Block.validateTransactions(previousHeader, transactions));
    }
}
