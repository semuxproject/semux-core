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
import org.semux.utils.ArrayUtil;
import org.semux.utils.Bytes;

public class PendingManagerTest {

    private static Blockchain chain;
    private static PendingManager pendingMgr;
    private static ChannelManager channelMgr;

    private static AccountState accountState;

    private static TransactionType type = TransactionType.TRANSFER;
    private static byte[] from = new EdDSA().toAddress();
    private static byte[] to = new EdDSA().toAddress();
    private static long value = 1 * Unit.MILLI_SEM;
    private static long fee = Config.MIN_TRANSACTION_FEE_SOFT;

    private static EdDSA key = new EdDSA();

    @BeforeClass
    public static void setup() {
        chain = new BlockchainImpl(MemoryDB.FACTORY);
        channelMgr = new ChannelManager();

        accountState = chain.getAccountState();
        accountState.getAccount(from).setAvailable(10000 * Unit.SEM);
    }

    @Before
    public void start() {
        pendingMgr = new PendingManager(chain, channelMgr);
        pendingMgr.start();
    }

    @Test
    public void testGetTransaction() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getTransactions().size());
    }

    @Test
    public void testAddTransaction() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);
        Transaction tx2 = new Transaction(type, from, to, value, fee, nonce + 128, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx2);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getTransactions().size());
    }

    @Test
    public void testNonceJump() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx3 = new Transaction(type, from, to, value, fee, nonce + 2, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx3);
        Transaction tx2 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx2);

        Thread.sleep(100);
        assertEquals(0, pendingMgr.getTransactions().size());

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);

        Thread.sleep(100);
        assertEquals(3, pendingMgr.getTransactions().size());
    }

    @Test
    public void testHighVolumeTransaction() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        int[] perm = ArrayUtil.permutation(5000);
        for (int p : perm) {
            Transaction tx = new Transaction(type, from, to, value, fee, nonce + p, now, Bytes.EMPY_BYTES).sign(key);
            pendingMgr.addTransaction(tx);
        }

        Thread.sleep(8000);
        assertEquals(perm.length, pendingMgr.getTransactions().size());
    }

    @Test
    public void testNewBlock() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);
        Transaction tx2 = new Transaction(type, from, to, value, fee, nonce + 1, now, Bytes.EMPY_BYTES).sign(key);
        // pendingMgr.addTransaction(tx3);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getTransactions().size());

        long number = 1;
        byte[] coinbase = Bytes.random(20);
        byte[] prevHash = Bytes.random(20);
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = Bytes.random(32);
        byte[] resultsRoot = Bytes.random(32);
        byte[] stateRoot = Bytes.random(32);
        byte[] data = {};
        List<Transaction> transactions = Arrays.asList(tx, tx2);
        List<TransactionResult> results = Arrays.asList(new TransactionResult(true), new TransactionResult(true));
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header.sign(new EdDSA()), transactions, results);
        chain.getAccountState().getAccount(from).setNonce(nonce + 2);
        pendingMgr.onBlockAdded(block);

        Transaction tx3 = new Transaction(type, from, to, value, fee, nonce + 2, now, Bytes.EMPY_BYTES).sign(key);
        pendingMgr.addTransaction(tx3);

        Thread.sleep(100);
        assertArrayEquals(tx3.getHash(), pendingMgr.getTransactions().get(0).getHash());
    }

    @After
    public void stop() {
        pendingMgr.stop();
    }
}
