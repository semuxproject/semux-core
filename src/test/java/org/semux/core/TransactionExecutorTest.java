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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.semux.Config;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;
import org.semux.utils.Bytes;

public class TransactionExecutorTest {

    private Blockchain chain;
    private AccountState as;
    private DelegateState ds;
    private TransactionExecutor exec;

    @Before
    public void prepare() {
        chain = new BlockchainImpl(MemoryDB.FACTORY);
        as = chain.getAccountState();
        ds = chain.getDeleteState();
        exec = new TransactionExecutor();
    }

    @Test
    public void testTransfer() {
        EdDSA key = new EdDSA();

        TransactionType type = TransactionType.TRANSFER;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        long value = 5;
        long fee = Config.MIN_TRANSACTION_FEE;
        long nonce = as.getAccount(from).getNonce() + 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        tx.sign(key);
        assertTrue(tx.validate());

        // insufficient balance
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), false);
        assertFalse(result.isValid());
        assertNotEquals(0, result.getCode());

        long balance = 1000 * Unit.SEM;
        as.getAccount(key.toAddress()).setBalance(balance);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track(), false);
        assertTrue(result.isValid());
        assertEquals(0, result.getCode());
        assertEquals(balance, as.getAccount(key.toAddress()).getBalance());
        assertEquals(0, as.getAccount(to).getBalance());

        // execute and commit
        result = exec.execute(tx, as.track(), ds.track(), true);
        assertTrue(result.isValid());
        assertEquals(0, result.getCode());
        assertEquals(balance - value - fee, as.getAccount(key.toAddress()).getBalance());
        assertEquals(value, as.getAccount(to).getBalance());
    }

    @Test
    public void testDelegate() {
        EdDSA delegate = new EdDSA();

        long balance = 2000 * Unit.SEM;
        as.getAccount(delegate.toAddress()).setBalance(balance);

        TransactionType type = TransactionType.DELEGATE;
        byte[] from = delegate.toAddress();
        byte[] to = Bytes.random(20);
        long value = Config.MIN_DELEGATE_FEE;
        long fee = Config.MIN_TRANSACTION_FEE;
        long nonce = as.getAccount(from).getNonce() + 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // register delegate (from != to, random name)
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), false);
        assertTrue(result.isValid());
        assertNotEquals(0, result.getCode());

        // register delegate (from == to, random name)
        tx = new Transaction(type, from, from, value, fee, nonce, timestamp, data);
        result = exec.execute(tx, as.track(), ds.track(), false);
        assertTrue(result.isValid());
        assertNotEquals(0, result.getCode());

        // register delegate (from == to, normal name) and commit
        data = Bytes.of("test");
        tx = new Transaction(type, from, from, value, fee, nonce, timestamp, data);
        result = exec.execute(tx, as.track(), ds.track(), true);
        assertTrue(result.isValid());
        assertEquals(0, result.getCode());

        // check state afterwards
        assertEquals(balance - Config.MIN_DELEGATE_FEE - fee, as.getAccount(delegate.toAddress()).getBalance());
        assertArrayEquals(delegate.toAddress(), ds.getDelegateByName(data).getAddress());
        assertArrayEquals(data, ds.getDelegateByAddress(delegate.toAddress()).getName());
    }

    @Test
    public void testVote() {
        EdDSA voter = new EdDSA();
        EdDSA delegate = new EdDSA();

        long balance = 100 * Unit.SEM;
        Account voterAcc = as.getAccount(voter.toAddress());
        voterAcc.setBalance(balance);

        TransactionType type = TransactionType.VOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        long value = balance / 3;
        long fee = Config.MIN_TRANSACTION_FEE;
        long nonce = as.getAccount(from).getNonce() + 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // vote for non-existing delegate
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), false);
        assertTrue(result.isValid());
        assertNotEquals(0, result.getCode());

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        // vote for delegate
        result = exec.execute(tx, as.track(), ds.track(), true);
        assertTrue(result.isValid());
        assertEquals(0, result.getCode());

        // check state afterwards
        assertEquals(balance - value - fee, voterAcc.getBalance());
        assertEquals(value, voterAcc.getLocked());
        assertEquals(value, ds.getDelegateByAddress(delegate.toAddress()).getVote());
    }

    @Test
    public void testUnvote() {
        EdDSA voter = new EdDSA();
        EdDSA delegate = new EdDSA();

        long balance = 100 * Unit.SEM;
        Account voterAcc = as.getAccount(voter.toAddress());
        voterAcc.setBalance(balance);

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        TransactionType type = TransactionType.UNVOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        long value = balance / 3;
        long fee = Config.MIN_TRANSACTION_FEE;
        long nonce = as.getAccount(from).getNonce() + 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // unvote (never voted before)
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), false);
        assertTrue(result.isValid());
        assertNotEquals(0, result.getCode());

        ds.vote(voter.toAddress(), delegate.toAddress(), value);

        // unvote (locked = 0)
        result = exec.execute(tx, as.track(), ds.track(), false);
        assertTrue(result.isValid());
        assertNotEquals(0, result.getCode());

        voterAcc.setLocked(value);

        // normal unvote
        result = exec.execute(tx, as.track(), ds.track(), true);
        assertTrue(result.isValid());
        assertEquals(0, result.getCode());

        // check state afterwards
        assertEquals(balance + value - fee, voterAcc.getBalance());
        assertEquals(0, voterAcc.getLocked());
        assertEquals(0, ds.getDelegateByAddress(delegate.toAddress()).getVote());
    }
}
