/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        exec = TransactionExecutor.getInstance();
    }

    @Test
    public void testTransfer() {
        EdDSA key = new EdDSA();

        TransactionType type = TransactionType.TRANSFER;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        long value = 5;
        long fee = Config.MIN_TRANSACTION_FEE;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        tx.sign(key);
        assertTrue(tx.validate());

        TransactionResult result = exec.execute(tx, as.track(), ds.track(), false);
        assertFalse(result.isSuccess());

        // add balance to that account
        long balance = 1000 * Unit.SEM;
        as.getAccount(key.toAddress()).setBalance(balance);

        assertTrue(exec.execute(tx, as.track(), ds.track(), false).isSuccess());
        assertEquals(balance, as.getAccount(key.toAddress()).getBalance());
        assertEquals(0, as.getAccount(to).getBalance());

        assertTrue(exec.execute(tx, as.track(), ds.track(), true).isSuccess());
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
        long value = Config.BFT_REGISTRATION_FEE;
        long fee = 1;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        assertFalse(exec.execute(tx, as.track(), ds.track(), false).isSuccess());

        to = from;
        tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        assertFalse(exec.execute(tx, as.track(), ds.track(), false).isSuccess());

        data = Bytes.of("test");
        tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        assertTrue(exec.execute(tx, as.track(), ds.track(), false).isSuccess());

        assertEquals(balance, as.getAccount(delegate.toAddress()).getBalance());
        exec.execute(tx, as.track(), ds.track(), true);
        assertEquals(balance - Config.BFT_REGISTRATION_FEE - fee, as.getAccount(delegate.toAddress()).getBalance());
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
        long fee = 1;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        assertFalse(exec.execute(tx, as.track(), ds.track(), true).isSuccess());

        ds.register(delegate.toAddress(), Bytes.of("delegate"));
        assertTrue(exec.execute(tx, as.track(), ds.track(), true).isSuccess());

        assertEquals(balance - value - 2 * fee, voterAcc.getBalance());
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
        long fee = 1;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        assertFalse(exec.execute(tx, as.track(), ds.track(), true).isSuccess());

        ds.vote(voter.toAddress(), delegate.toAddress(), value);
        assertFalse(exec.execute(tx, as.track(), ds.track(), true).isSuccess());

        voterAcc.setLocked(value);
        assertTrue(exec.execute(tx, as.track(), ds.track(), true).isSuccess());

        assertEquals(balance + value - 3 * fee, voterAcc.getBalance());
        assertEquals(0, voterAcc.getLocked());
        assertEquals(0, ds.getDelegateByAddress(delegate.toAddress()).getVote());
    }
}
