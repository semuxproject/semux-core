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
        ds = chain.getDelegateState();
        exec = new TransactionExecutor();
    }

    private TransactionResult executeAndCommit(TransactionExecutor exec, Transaction tx, AccountState as,
            DelegateState ds) {
        TransactionResult res = exec.execute(tx, as, ds);
        as.commit();
        ds.commit();

        return res;
    }

    @Test
    public void testTransfer() {
        EdDSA key = new EdDSA();

        TransactionType type = TransactionType.TRANSFER;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        long value = 5;
        long fee = Config.MIN_TRANSACTION_FEE_HARD;
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        tx.sign(key);
        assertTrue(tx.validate());

        // insufficient available
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isValid());

        long available = 1000 * Unit.SEM;
        as.adjustAvailable(key.toAddress(), available);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track());
        assertTrue(result.isValid());
        assertEquals(available, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(0, as.getAccount(to).getAvailable());

        // execute and commit
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isValid());
        assertEquals(available - value - fee, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(value, as.getAccount(to).getAvailable());
    }

    @Test
    public void testDelegate() {
        EdDSA delegate = new EdDSA();

        long available = 2000 * Unit.SEM;
        as.adjustAvailable(delegate.toAddress(), available);

        TransactionType type = TransactionType.DELEGATE;
        byte[] from = delegate.toAddress();
        byte[] to = Bytes.random(20);
        long value = Config.DELEGATE_BURN_AMOUNT;
        long fee = Config.MIN_TRANSACTION_FEE_HARD;
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // register delegate (from != to, random name)
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isValid());

        // register delegate (from == to, random name)
        tx = new Transaction(type, from, from, value, fee, nonce, timestamp, data);
        result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isValid());

        // register delegate (from == to, normal name) and commit
        data = Bytes.of("test");
        tx = new Transaction(type, from, from, value, fee, nonce, timestamp, data);
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isValid());
        assertEquals(available - Config.DELEGATE_BURN_AMOUNT - fee, as.getAccount(delegate.toAddress()).getAvailable());
        assertArrayEquals(delegate.toAddress(), ds.getDelegateByName(data).getAddress());
        assertArrayEquals(data, ds.getDelegateByAddress(delegate.toAddress()).getName());
    }

    @Test
    public void testVote() {
        EdDSA voter = new EdDSA();
        EdDSA delegate = new EdDSA();

        long available = 100 * Unit.SEM;
        as.adjustAvailable(voter.toAddress(), available);

        TransactionType type = TransactionType.VOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        long value = available / 3;
        long fee = Config.MIN_TRANSACTION_FEE_HARD;
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // vote for non-existing delegate
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isValid());

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        // vote for delegate
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isValid());
        assertEquals(available - value - fee, as.getAccount(voter.toAddress()).getAvailable());
        assertEquals(value, as.getAccount(voter.toAddress()).getLocked());
        assertEquals(value, ds.getDelegateByAddress(delegate.toAddress()).getVotes());
    }

    @Test
    public void testUnvote() {
        EdDSA voter = new EdDSA();
        EdDSA delegate = new EdDSA();

        long available = 100 * Unit.SEM;
        as.adjustAvailable(voter.toAddress(), available);

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        TransactionType type = TransactionType.UNVOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        long value = available / 3;
        long fee = Config.MIN_TRANSACTION_FEE_HARD;
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // unvote (never voted before)
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isValid());

        ds.vote(voter.toAddress(), delegate.toAddress(), value);

        // unvote (locked = 0)
        result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isValid());

        as.adjustLocked(voter.toAddress(), value);

        // normal unvote
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isValid());
        assertEquals(available + value - fee, as.getAccount(voter.toAddress()).getAvailable());
        assertEquals(0, as.getAccount(voter.toAddress()).getLocked());
        assertEquals(0, ds.getDelegateByAddress(delegate.toAddress()).getVotes());
    }
}
