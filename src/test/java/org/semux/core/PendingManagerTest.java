/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.Config;
import org.semux.core.state.AccountState;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;
import org.semux.net.ChannelManager;
import org.semux.utils.Bytes;

public class PendingManagerTest {

    private static Blockchain chain;
    private static PendingManager pendingMgr;
    private static ChannelManager channelMgr;

    private static AccountState accountState;

    private static TransactionType type = TransactionType.TRANSFER;
    private static byte[] from = new EdDSA().toAddress();
    private static byte[] to = new EdDSA().toAddress();
    private static long value = 100 * Unit.MILLI_SEM;
    private static long fee = Config.MIN_TRANSACTION_FEE;

    private static EdDSA key = new EdDSA();

    @BeforeClass
    public static void setup() {
        chain = new BlockchainImpl(MemoryDB.FACTORY);
        channelMgr = new ChannelManager();

        accountState = chain.getAccountState();
        accountState.getAccount(from).setBalance(2 * Unit.SEM);
    }

    @Before
    public void start() {
        pendingMgr = new PendingManager();
        pendingMgr.start(chain, channelMgr);
    }

    @Test
    public void testGetTransaction() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx1 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx1);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getTransactions().size());
    }

    @Test
    public void testAddTransaction() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx1 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx1);
        Transaction tx128 = new Transaction(type, from, to, value, fee, nonce + 128, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx128);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getTransactions().size());
    }

    @Test
    public void testRemoveTransactions() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx1 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx1);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getTransactions().size());

        pendingMgr.removeTransaction(tx1);
        assertEquals(0, pendingMgr.getTransactions().size());
    }

    @Test
    public void testNonceJump() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx3 = new Transaction(type, from, to, value, fee, nonce + 3, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx3);
        Transaction tx2 = new Transaction(type, from, to, value, fee, nonce + 2, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx2);

        Thread.sleep(100);
        assertEquals(0, pendingMgr.getTransactions().size());

        Transaction tx1 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx1);

        Thread.sleep(100);
        assertEquals(3, pendingMgr.getTransactions().size());
    }

    @Test
    public void testNewBlock() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx1 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx1);
        Transaction tx2 = new Transaction(type, from, to, value, fee, nonce + 2, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx2);

        Thread.sleep(100);
        assertEquals(2, pendingMgr.getTransactions().size());

        long number = 1;
        byte[] coinbase = Bytes.random(20);
        byte[] prevHash = Bytes.random(20);
        long timestamp = System.currentTimeMillis();
        byte[] merkleRoot = Bytes.random(32);
        byte[] data = {};
        List<Transaction> transactions = Arrays.asList(tx1, tx2);
        Block block = new Block(number, coinbase, prevHash, timestamp, merkleRoot, data, transactions);
        pendingMgr.onBlockAdded(block);

        Transaction tx3 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.of("BAD")).sign(key);
        pendingMgr.addTransaction(tx3);

        Thread.sleep(100);
        assertArrayEquals(Bytes.of("BAD"), pendingMgr.getTransactions().get(0).getData());
    }

    @After
    public void stop() {
        pendingMgr.stop();
    }
}
