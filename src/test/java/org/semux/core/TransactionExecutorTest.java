/**
 * Copyright (c) 2017-2018 The Semux Developers
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
import org.junit.Rule;
import org.junit.Test;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Key;
import org.semux.rules.TemporaryDbRule;
import org.semux.util.Bytes;

public class TransactionExecutorTest {

    @Rule
    public TemporaryDbRule temporaryDBFactory = new TemporaryDbRule();

    private Config config;
    private Blockchain chain;
    private AccountState as;
    private DelegateState ds;
    private TransactionExecutor exec;
    private Network network;

    @Before
    public void prepare() {
        config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);
        chain = new BlockchainImpl(config, temporaryDBFactory);
        as = chain.getAccountState();
        ds = chain.getDelegateState();
        exec = new TransactionExecutor(config);
        network = config.network();
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
        Key key = new Key();

        TransactionType type = TransactionType.TRANSFER;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        long value = 5;
        long fee = config.minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);
        assertTrue(tx.validate(network));

        // insufficient available
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isSuccess());

        long available = 1000 * Unit.SEM;
        as.adjustAvailable(key.toAddress(), available);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track());
        assertTrue(result.isSuccess());
        assertEquals(available, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(0, as.getAccount(to).getAvailable());

        // execute and commit
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isSuccess());
        assertEquals(available - value - fee, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(value, as.getAccount(to).getAvailable());
    }

    @Test
    public void testDelegate() {
        Key delegate = new Key();

        long available = 2000 * Unit.SEM;
        as.adjustAvailable(delegate.toAddress(), available);

        TransactionType type = TransactionType.DELEGATE;
        byte[] from = delegate.toAddress();
        byte[] to = Bytes.random(20);
        long value = config.minDelegateBurnAmount();
        long fee = config.minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // register delegate (to != EMPTY_ADDRESS, random name)
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(delegate);
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isSuccess());

        // register delegate (to == EMPTY_ADDRESS, random name)
        tx = new Transaction(network, type, Bytes.EMPTY_ADDRESS, value, fee, nonce, timestamp, data).sign(delegate);
        result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isSuccess());

        // register delegate (to == EMPTY_ADDRESS, normal name) and commit
        data = Bytes.of("test");
        tx = new Transaction(network, type, Bytes.EMPTY_ADDRESS, value, fee, nonce, timestamp, data).sign(delegate);
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isSuccess());
        assertEquals(available - config.minDelegateBurnAmount() - fee,
                as.getAccount(delegate.toAddress()).getAvailable());
        assertArrayEquals(delegate.toAddress(), ds.getDelegateByName(data).getAddress());
        assertArrayEquals(data, ds.getDelegateByAddress(delegate.toAddress()).getName());
    }

    @Test
    public void testVote() {
        Key voter = new Key();
        Key delegate = new Key();

        long available = 100 * Unit.SEM;
        as.adjustAvailable(voter.toAddress(), available);

        TransactionType type = TransactionType.VOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        long value = available / 3;
        long fee = config.minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = {};

        // vote for non-existing delegate
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(voter);
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isSuccess());

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        // vote for delegate
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isSuccess());
        assertEquals(available - value - fee, as.getAccount(voter.toAddress()).getAvailable());
        assertEquals(value, as.getAccount(voter.toAddress()).getLocked());
        assertEquals(value, ds.getDelegateByAddress(delegate.toAddress()).getVotes());
    }

    @Test
    public void testUnvote() {
        Key voter = new Key();
        Key delegate = new Key();

        long available = 100 * Unit.SEM;
        as.adjustAvailable(voter.toAddress(), available);

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        TransactionType type = TransactionType.UNVOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        long value = available / 3;
        long fee = config.minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = System.currentTimeMillis();
        byte[] data = {};

        // unvote (never voted before)
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(voter);
        TransactionResult result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isSuccess());

        ds.vote(voter.toAddress(), delegate.toAddress(), value);

        // unvote (locked = 0)
        result = exec.execute(tx, as.track(), ds.track());
        assertFalse(result.isSuccess());

        as.adjustLocked(voter.toAddress(), value);

        // normal unvote
        result = executeAndCommit(exec, tx, as.track(), ds.track());
        assertTrue(result.isSuccess());
        assertEquals(available + value - fee, as.getAccount(voter.toAddress()).getAvailable());
        assertEquals(0, as.getAccount(voter.toAddress()).getLocked());
        assertEquals(0, ds.getDelegateByAddress(delegate.toAddress()).getVotes());
    }

    @Test
    public void testValidateDelegateName() {
        assertFalse(TransactionExecutor.validateDelegateName(Bytes.random(2)));
        assertFalse(TransactionExecutor.validateDelegateName(Bytes.random(17)));
        assertFalse(TransactionExecutor.validateDelegateName(new byte[] { 0x11, 0x22, 0x33 }));

        int[][] ranges = { { 'a', 'z' }, { '0', '9' }, { '_', '_' } };
        for (int[] range : ranges) {
            for (int i = range[0]; i <= range[1]; i++) {
                byte[] data = new byte[3];
                data[0] = (byte) i;
                data[1] = (byte) i;
                data[2] = (byte) i;
                assertTrue(TransactionExecutor.validateDelegateName(data));
            }
        }
    }
}
