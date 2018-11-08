/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.semux.core.Amount.Unit.MILLI_SEM;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.PendingManager.ALLOWED_TIME_DRIFT;
import static org.semux.core.TransactionResult.Code.INVALID_TIMESTAMP;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.semux.KernelMock;
import org.semux.Network;
import org.semux.config.Constants;
import org.semux.core.state.AccountState;
import org.semux.crypto.Key;
import org.semux.db.LeveldbDatabase.LeveldbFactory;
import org.semux.net.ChannelManager;
import org.semux.rules.KernelRule;
import org.semux.util.ArrayUtil;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

public class PendingManagerTest {

    private static KernelMock kernel;
    private static PendingManager pendingMgr;

    private static AccountState accountState;

    private static Key key = new Key();
    private static Network network;
    private static TransactionType type = TransactionType.TRANSFER;
    private static byte[] from = key.toAddress();
    private static byte[] to = new Key().toAddress();
    private static Amount value = MILLI_SEM.of(1);
    private static Amount fee;

    @ClassRule
    public static KernelRule kernelRule = new KernelRule(51610, 51710);

    @BeforeClass
    public static void setUp() {
        kernel = kernelRule.getKernel();

        kernel.setBlockchain(
                new BlockchainImpl(kernel.getConfig(), new LeveldbFactory(kernel.getConfig().databaseDir())));
        kernel.setChannelManager(new ChannelManager(kernel));

        accountState = kernel.getBlockchain().getAccountState();
        accountState.adjustAvailable(from, SEM.of(10000));

        network = kernel.getConfig().network();
        fee = kernel.getConfig().minTransactionFee();
    }

    @Before
    public void start() {
        pendingMgr = new PendingManager(kernel);
        pendingMgr.start();
    }

    @Test
    public void testGetTransaction() throws InterruptedException {
        long now = TimeUtil.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getPendingTransactions().size());
    }

    @Test
    public void testAddTransaction() throws InterruptedException {
        long now = TimeUtil.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);
        Transaction tx2 = new Transaction(network, type, to, value, fee, nonce + 128, now, Bytes.EMPTY_BYTES)
                .sign(key);
        pendingMgr.addTransaction(tx2);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getPendingTransactions().size());
    }

    @Test
    public void testAddTransactionSyncErrorInvalidFormat() {
        Transaction tx = new Transaction(network, type, to, value, fee, 0, 0, Bytes.EMPTY_BYTES).sign(key);
        PendingManager.ProcessingResult result = pendingMgr.addTransactionSync(tx);
        assertEquals(0, pendingMgr.getPendingTransactions().size());
        assertNotNull(result.error);
        assertEquals(TransactionResult.Code.INVALID_FORMAT, result.error);
    }

    @Test
    public void testAddTransactionSyncErrorDuplicatedHash() {
        Transaction tx = new Transaction(network, type, to, value, fee, 0, TimeUtil.currentTimeMillis(),
                Bytes.EMPTY_BYTES)
                        .sign(key);

        kernel.setBlockchain(spy(kernel.getBlockchain()));
        doReturn(true).when(kernel.getBlockchain()).hasTransaction(tx.getHash());

        PendingManager.ProcessingResult result = pendingMgr.addTransactionSync(tx);
        assertEquals(0, pendingMgr.getPendingTransactions().size());
        assertNotNull(result.error);
        assertEquals(TransactionResult.Code.DUPLICATE_TRANSACTION, result.error);

        Mockito.reset(kernel.getBlockchain());
    }

    @Test
    public void testAddTransactionSyncInvalidRecipient() {
        Transaction tx = new Transaction(network, type, Constants.COINBASE_KEY.toAddress(), value, fee, 0,
                TimeUtil.currentTimeMillis(),
                Bytes.EMPTY_BYTES)
                        .sign(key);

        PendingManager.ProcessingResult result = pendingMgr.addTransactionSync(tx);
        assertEquals(0, pendingMgr.getPendingTransactions().size());
        assertNotNull(result.error);
        assertEquals(TransactionResult.Code.INVALID_FORMAT, result.error);
    }

    @Test
    public void testNonceJump() throws InterruptedException {
        long now = TimeUtil.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx3 = new Transaction(network, type, to, value, fee, nonce + 2, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx3);
        Transaction tx2 = new Transaction(network, type, to, value, fee, nonce + 1, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx2);

        Thread.sleep(100);
        assertEquals(0, pendingMgr.getPendingTransactions().size());

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);

        Thread.sleep(100);
        assertEquals(3, pendingMgr.getPendingTransactions().size());
    }

    @Test
    public void testNonceJumpTimestampError() throws InterruptedException {
        long now = TimeUtil.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx3 = new Transaction(network, type, to, value, fee, nonce + 2,
                now - TimeUnit.HOURS.toMillis(2) + TimeUnit.SECONDS.toMillis(1),
                Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx3);
        Transaction tx2 = new Transaction(network, type, to, value, fee, nonce + 1, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx2);

        TimeUnit.SECONDS.sleep(1);
        assertEquals(0, pendingMgr.getPendingTransactions().size());

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);

        TimeUnit.SECONDS.sleep(1);
        List<PendingManager.PendingTransaction> txs = pendingMgr.getPendingTransactions();
        assertEquals(3, txs.size());
    }

    @Test
    public void testTimestampError() {
        long now = TimeUtil.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx3 = new Transaction(network, type, to, value, fee, nonce, now - ALLOWED_TIME_DRIFT - 1,
                Bytes.EMPTY_BYTES).sign(key);
        PendingManager.ProcessingResult result = pendingMgr.addTransactionSync(tx3);
        assertEquals(INVALID_TIMESTAMP, result.error);
    }

    @Test
    public void testHighVolumeTransaction() throws InterruptedException {
        long now = TimeUtil.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        int[] perm = ArrayUtil.permutation(5000);
        for (int p : perm) {
            Transaction tx = new Transaction(network, type, to, value, fee, nonce + p, now, Bytes.EMPTY_BYTES)
                    .sign(key);
            pendingMgr.addTransaction(tx);
        }

        Thread.sleep(8000);
        assertEquals(perm.length, pendingMgr.getPendingTransactions().size());
    }

    @Test
    public void testNewBlock() throws InterruptedException {
        long now = TimeUtil.currentTimeMillis();
        long nonce = accountState.getAccount(from).getNonce();

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx);
        Transaction tx2 = new Transaction(network, type, to, value, fee, nonce + 1, now, Bytes.EMPTY_BYTES).sign(key);
        // pendingMgr.addTransaction(tx3);

        Thread.sleep(100);
        assertEquals(1, pendingMgr.getPendingTransactions().size());

        long number = 1;
        byte[] coinbase = Bytes.random(20);
        byte[] prevHash = Bytes.random(20);
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] transactionsRoot = Bytes.random(32);
        byte[] resultsRoot = Bytes.random(32);
        byte[] stateRoot = Bytes.random(32);
        byte[] data = {};
        List<Transaction> transactions = Arrays.asList(tx, tx2);
        List<TransactionResult> results = Arrays.asList(new TransactionResult(), new TransactionResult());
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, transactions, results);
        kernel.getBlockchain().getAccountState().increaseNonce(from);
        kernel.getBlockchain().getAccountState().increaseNonce(from);
        pendingMgr.onBlockAdded(block);

        Transaction tx3 = new Transaction(network, type, to, value, fee, nonce + 2, now, Bytes.EMPTY_BYTES).sign(key);
        pendingMgr.addTransaction(tx3);

        Thread.sleep(100);
        assertArrayEquals(tx3.getHash(), pendingMgr.getPendingTransactions().get(0).transaction.getHash());
    }

    @After
    public void stop() {
        pendingMgr.stop();
    }
}
