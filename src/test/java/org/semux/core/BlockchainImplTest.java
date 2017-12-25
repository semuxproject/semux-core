/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.core.BlockchainImpl.StatsType;
import org.semux.crypto.EdDSA;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

public class BlockchainImplTest {

    @Rule
    public TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    private Config config;
    private BlockchainImpl chain;

    private byte[] coinbase = Bytes.random(30);
    private byte[] prevHash = Bytes.random(32);

    private EdDSA key = new EdDSA();
    private byte[] from = key.toAddress();
    private byte[] to = Bytes.random(20);
    private long value = 20;
    private long fee = 1;
    private long nonce = 12345;
    private byte[] data = Bytes.of("test");
    private long timestamp = System.currentTimeMillis() - 60 * 1000;
    private Transaction tx = new Transaction(TransactionType.TRANSFER, to, value, fee, nonce, timestamp, data)
            .sign(key);
    private TransactionResult res = new TransactionResult(true);

    @Before
    public void setUp() {
        config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);
        chain = new BlockchainImpl(config, temporaryDBFactory);
    }

    @Test
    public void testGetLatestBlock() {
        assertEquals(0, chain.getLatestBlockNumber());
        assertNotNull(chain.getLatestBlockHash());
        assertNotNull(chain.getLatestBlock());

        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        assertNotEquals(0, chain.getLatestBlockNumber());
        assertTrue(chain.getLatestBlock().getNumber() == newBlock.getNumber());
    }

    @Test
    public void testGetLatestBlockHash() {
        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        assertEquals(newBlock.getNumber(), chain.getLatestBlockNumber());
        assertArrayEquals(newBlock.getHash(), chain.getLatestBlockHash());
    }

    @Test
    public void testGetBlock() {
        assertEquals(0, chain.getBlock(0).getNumber());
        assertNull(chain.getBlock(1));

        long number = 1;
        Block newBlock = createBlock(number);
        chain.addBlock(newBlock);

        assertTrue(chain.getBlock(number).getNumber() == number);
        assertTrue(chain.getBlock(newBlock.getHash()).getNumber() == number);
    }

    @Test
    public void testGetBlockNumber() {
        long number = 1;
        Block newBlock = createBlock(number);
        chain.addBlock(newBlock);

        assertEquals(number, chain.getBlockNumber(newBlock.getHash()));
    }

    @Test
    public void testGetGenesis() {
        assertArrayEquals(Genesis.load(config.dataDir()).getHash(), chain.getGenesis().getHash());
    }

    @Test
    public void testGetBlockHeader() {
        assertArrayEquals(Genesis.load(config.dataDir()).getHash(), chain.getBlockHeader(0).getHash());

        long number = 1;
        Block newBlock = createBlock(number);
        chain.addBlock(newBlock);

        assertArrayEquals(newBlock.getHash(), chain.getBlockHeader(1).getHash());
        assertEquals(newBlock.getNumber(), chain.getBlockHeader(newBlock.getHash()).getNumber());
    }

    @Test
    public void testGetTransaction() {
        assertNull(chain.getTransaction(tx.getHash()));

        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        Transaction t = chain.getTransaction(tx.getHash());
        assertNotNull(t);
        assertTrue(Arrays.equals(from, t.getFrom()));
        assertTrue(Arrays.equals(to, t.getTo()));
        assertTrue(Arrays.equals(data, t.getData()));
        assertTrue(t.getValue() == value);
        assertTrue(t.getNonce() == nonce);
        assertTrue(t.getTimestamp() == timestamp);
    }

    @Test
    public void testGetTransactionResult() {
        assertNull(chain.getTransaction(tx.getHash()));

        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        TransactionResult r = chain.getTransactionResult(tx.getHash());
        assertArrayEquals(res.toBytes(), r.toBytes());
    }

    @Test
    public void testGetTransactionBlockNumber() {
        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        assertEquals(newBlock.getNumber(), chain.getTransactionBlockNumber(tx.getHash()));
    }

    @Test
    public void testGetTransactionCount() {
        assertNull(chain.getTransaction(tx.getHash()));

        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        assertEquals(1, chain.getTransactionCount(tx.getFrom()));
    }

    @Test
    public void testGetAccountTransactions() {
        assertNull(chain.getTransaction(tx.getHash()));

        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        List<Transaction> txs = chain.getTransactions(tx.getFrom(), 0, 100);
        assertEquals(1, txs.size());
        assertArrayEquals(tx.toBytes(), txs.get(0).toBytes());
    }

    @Test
    public void testSerialization() {
        Block block1 = createBlock(1);

        Block block2 = Block.fromBytes(block1.toBytesHeader(), block1.toBytesTransactions(), block1.toBytesResults(),
                block1.toBytesVotes());
        assertArrayEquals(block1.getHash(), block2.getHash());
        assertArrayEquals(block1.getCoinbase(), block2.getCoinbase());
        assertArrayEquals(block1.getParentHash(), block2.getParentHash());
        assertEquals(block1.getNumber(), block2.getNumber());

        assertEquals(block1.getTransactions().size(), block2.getTransactions().size());
    }

    @Test
    public void testGetTransactions() {
        Block block = createBlock(1);
        chain.addBlock(block);

        List<Transaction> list = chain.getTransactions(from, 0, 1024);
        assertEquals(1, list.size());
        assertArrayEquals(tx.getHash(), list.get(0).getHash());

        list = chain.getTransactions(to, 0, 1024);
        assertEquals(1, list.size());
        assertArrayEquals(tx.getHash(), list.get(0).getHash());
    }

    @Test
    public void testValidatorStates() {
        byte[] address = Bytes.random(20);

        assertEquals(0, chain.getValidatorStats(address).getBlocksForged());
        assertEquals(0, chain.getValidatorStats(address).getTurnsHit());
        assertEquals(0, chain.getValidatorStats(address).getTurnsMissed());

        chain.adjustValidatorStats(address, StatsType.FORGED, 1);
        assertEquals(1, chain.getValidatorStats(address).getBlocksForged());

        chain.adjustValidatorStats(address, StatsType.HIT, 1);
        assertEquals(1, chain.getValidatorStats(address).getTurnsHit());

        chain.adjustValidatorStats(address, StatsType.MISSED, 1);
        assertEquals(1, chain.getValidatorStats(address).getTurnsMissed());
        chain.adjustValidatorStats(address, StatsType.MISSED, 1);
        assertEquals(2, chain.getValidatorStats(address).getTurnsMissed());
    }

    private Block createBlock(long number) {
        return createBlock(number, Collections.singletonList(tx), Collections.singletonList(res));
    }

    private Block createBlock(long number, List<Transaction> transactions, List<TransactionResult> results) {
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(transactions);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(results);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = Bytes.of("test");
        long timestamp = System.currentTimeMillis();

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        return new Block(header, transactions, results);
    }
}
