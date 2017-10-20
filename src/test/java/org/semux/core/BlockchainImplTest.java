/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hash;
import org.semux.db.MemoryDB;
import org.semux.utils.Bytes;
import org.semux.utils.MerkleUtil;

public class BlockchainImplTest {

    private byte[] coinbase = Bytes.random(30);;
    private byte[] prevHash = Bytes.random(32);;

    private byte[] from = Bytes.random(20);
    private byte[] to = Bytes.random(20);
    private long value = 20;
    private long fee = 1;
    private long nonce = 12345;
    private byte[] data = Bytes.of("test");
    private long timestamp = System.currentTimeMillis() - 60 * 1000;
    private Transaction tx = new Transaction(TransactionType.TRANSFER, from, to, value, fee, nonce, timestamp, data);
    private TransactionResult res = new TransactionResult(true);
    {
        tx.sign(new EdDSA());
    }

    @Test
    public void testGetLatest() {
        Blockchain chain = createBlockchain();

        assertTrue(chain.getLatestBlockNumber() == 0);
        assertNotNull(chain.getLatestBlockHash());
        assertNotNull(chain.getLatestBlock());

        Block newBlock = createBlock(1);
        chain.addBlock(newBlock);

        assertFalse(chain.getLatestBlock().isGensis());
        assertTrue(chain.getLatestBlock().getNumber() == newBlock.getNumber());
    }

    @Test
    public void testGetBlock() {
        Blockchain chain = createBlockchain();

        assertTrue(chain.getBlock(0).isGensis());
        assertTrue(chain.getBlock(0).getNumber() == 0);
        assertNull(chain.getBlock(1));

        long number = 1;
        Block newBlock = createBlock(number);
        chain.addBlock(newBlock);

        assertFalse(chain.getBlock(number).isGensis());
        assertTrue(chain.getBlock(number).getNumber() == number);
        assertTrue(chain.getBlock(newBlock.getHash()).getNumber() == number);
    }

    @Test
    public void testGetBlockHeader() {
        Blockchain chain = createBlockchain();

        assertArrayEquals(Genesis.getInstance().getHash(), chain.getBlockHeader(0).getHash());

        long number = 1;
        Block newBlock = createBlock(number);
        chain.addBlock(newBlock);

        assertArrayEquals(newBlock.getHash(), chain.getBlockHeader(1).getHash());
        assertEquals(newBlock.getNumber(), chain.getBlockHeader(newBlock.getHash()).getNumber());
    }

    @Test
    public void testGetTransaction() {
        Blockchain chain = createBlockchain();

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
    public void testSerialization() {
        Block block1 = createBlock(1);

        Block block2 = Block.fromBytes(block1.toBytes());
        assertArrayEquals(block1.getHash(), block2.getHash());
        assertArrayEquals(block1.getCoinbase(), block2.getCoinbase());
        assertArrayEquals(block1.getPrevHash(), block2.getPrevHash());
        assertEquals(block1.getNumber(), block2.getNumber());
        assertArrayEquals(block1.getSignature().toBytes(), block2.getSignature().toBytes());

        assertEquals(block1.getTransactions().size(), block2.getTransactions().size());
    }

    @Test
    public void testGetTransactions() {
        Block block = createBlock(1);
        Blockchain chain = createBlockchain();
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
        BlockchainImpl chain = createBlockchain();
        byte[] address = Bytes.random(20);

        assertEquals(0, chain.getNumberOfBlocksForged(address));
        assertEquals(0, chain.getNumberOfTurnsHit(address));
        assertEquals(0, chain.getNumberOfTurnsMissed(address));
        chain.updateValidatorStats(address, Bytes.of("forged"), 1);
        assertEquals(1, chain.getNumberOfBlocksForged(address));
        chain.updateValidatorStats(address, Bytes.of("hit"), 1);
        assertEquals(1, chain.getNumberOfTurnsHit(address));
        chain.updateValidatorStats(address, Bytes.of("missed"), 1);
        assertEquals(1, chain.getNumberOfTurnsMissed(address));
        chain.updateValidatorStats(address, Bytes.of("missed"), 1);
        assertEquals(2, chain.getNumberOfTurnsMissed(address));
    }

    private BlockchainImpl createBlockchain() {
        return new BlockchainImpl(MemoryDB.FACTORY);
    }

    private Block createBlock(long number) {
        return createBlock(number, Collections.singletonList(tx), Collections.singletonList(res));
    }

    private Block createBlock(long number, List<Transaction> transactions, List<TransactionResult> results) {
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(transactions);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(results);
        byte[] stateRoot = Hash.EMPTY_H256;
        byte[] data = Bytes.of("test");
        long timestamp = System.currentTimeMillis();

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        return new Block(header.sign(new EdDSA()), transactions, results);
    }
}
