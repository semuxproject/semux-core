/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.semux.core.Amount.ZERO;
import static org.semux.core.TransactionResult.Code.INSUFFICIENT_AVAILABLE;
import static org.semux.core.TransactionResult.Code.INSUFFICIENT_LOCKED;
import static org.semux.core.Unit.SEM;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.UnitTestnetConfig;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Key;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxBlockStore;

public class TransactionExecutorTest {

    @Rule
    public TemporaryDatabaseRule temporaryDBFactory = new TemporaryDatabaseRule();

    private Config config;
    private Blockchain chain;
    private AccountState as;
    private DelegateState ds;
    private TransactionExecutor exec;
    private Network network;
    private SemuxBlock block = null;

    @Before
    public void prepare() {
        config = new UnitTestnetConfig(Constants.DEFAULT_ROOT_DIR);
        chain = new BlockchainImpl(config, temporaryDBFactory);
        as = chain.getAccountState();
        ds = chain.getDelegateState();
        exec = new TransactionExecutor(config, new SemuxBlockStore(chain), chain.isVMEnabled(),
                chain.isVotingPrecompiledUpgraded());
        network = config.network();
        block = new SemuxBlock(mock(BlockHeader.class), config.spec().maxBlockGasLimit());
    }

    private TransactionResult executeAndCommit(TransactionExecutor exec, Transaction tx, AccountState as,
            DelegateState ds, SemuxBlock bh) {
        TransactionResult res = exec.execute(tx, as, ds, bh, 0);
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
        Amount value = Amount.of(5);
        Amount fee = config.spec().minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);
        assertTrue(tx.validate(network));

        // insufficient available
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertFalse(result.getCode().isSuccess());

        Amount available = Amount.of(1000, SEM);
        as.adjustAvailable(key.toAddress(), available);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertTrue(result.getCode().isSuccess());
        assertEquals(available, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(ZERO, as.getAccount(to).getAvailable());

        // execute and commit
        result = executeAndCommit(exec, tx, as.track(), ds.track(), block);
        assertTrue(result.getCode().isSuccess());
        assertEquals(available.subtract(value.add(fee)), as.getAccount(key.toAddress()).getAvailable());
        assertEquals(value, as.getAccount(to).getAvailable());
    }

    @Test
    public void testDelegate() {
        Key delegate = new Key();

        Amount available = Amount.of(2000, SEM);
        as.adjustAvailable(delegate.toAddress(), available);

        TransactionType type = TransactionType.DELEGATE;
        byte[] from = delegate.toAddress();
        byte[] to = Bytes.random(20);
        Amount value = config.spec().minDelegateBurnAmount();
        Amount fee = config.spec().minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = Bytes.random(16);

        // register delegate (to != EMPTY_ADDRESS, random name)
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(delegate);
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertFalse(result.getCode().isSuccess());

        // register delegate (to == EMPTY_ADDRESS, random name)
        tx = new Transaction(network, type, Bytes.EMPTY_ADDRESS, value, fee, nonce, timestamp, data).sign(delegate);
        result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertFalse(result.getCode().isSuccess());

        // register delegate (to == EMPTY_ADDRESS, normal name) and commit
        data = Bytes.of("test");
        tx = new Transaction(network, type, Bytes.EMPTY_ADDRESS, value, fee, nonce, timestamp, data).sign(delegate);

        result = executeAndCommit(exec, tx, as.track(), ds.track(), block);
        assertTrue(result.getCode().isSuccess());
        assertEquals(available.subtract(config.spec().minDelegateBurnAmount().add(fee)),
                as.getAccount(delegate.toAddress()).getAvailable());
        assertArrayEquals(delegate.toAddress(), ds.getDelegateByName(data).getAddress());
        assertArrayEquals(data, ds.getDelegateByAddress(delegate.toAddress()).getName());
    }

    @Test
    public void testVote() {
        Key voter = new Key();
        Key delegate = new Key();

        Amount available = Amount.of(100, SEM);
        as.adjustAvailable(voter.toAddress(), available);

        TransactionType type = TransactionType.VOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        Amount value = Amount.of(33, SEM);
        Amount fee = config.spec().minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = {};

        // vote for non-existing delegate
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(voter);
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertFalse(result.getCode().isSuccess());

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        // vote for delegate
        result = executeAndCommit(exec, tx, as.track(), ds.track(), block);
        assertTrue(result.getCode().isSuccess());
        assertEquals(available.subtract(value.add(fee)), as.getAccount(voter.toAddress()).getAvailable());
        assertEquals(value, as.getAccount(voter.toAddress()).getLocked());
        assertEquals(value, ds.getDelegateByAddress(delegate.toAddress()).getVotes());
    }

    @Test
    public void testUnvote() {
        Key voter = new Key();
        Key delegate = new Key();

        Amount available = Amount.of(100, SEM);
        as.adjustAvailable(voter.toAddress(), available);

        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        TransactionType type = TransactionType.UNVOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        Amount value = Amount.of(33, SEM);
        Amount fee = config.spec().minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = {};

        // unvote (never voted before)
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(voter);
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertFalse(result.getCode().isSuccess());
        assertEquals(INSUFFICIENT_LOCKED, result.code);
        ds.vote(voter.toAddress(), delegate.toAddress(), value);

        // unvote (locked = 0)
        result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertFalse(result.getCode().isSuccess());
        assertEquals(INSUFFICIENT_LOCKED, result.code);

        as.adjustLocked(voter.toAddress(), value);

        // normal unvote
        result = executeAndCommit(exec, tx, as.track(), ds.track(), block);
        assertTrue(result.getCode().isSuccess());
        assertEquals(available.add(value.subtract(fee)), as.getAccount(voter.toAddress()).getAvailable());
        assertEquals(ZERO, as.getAccount(voter.toAddress()).getLocked());
        assertEquals(ZERO, ds.getDelegateByAddress(delegate.toAddress()).getVotes());
    }

    @Test
    public void testUnvoteInsufficientFee() {
        Key voter = new Key();
        Key delegate = new Key();

        as.adjustAvailable(voter.toAddress(), config.spec().minTransactionFee().subtract(Amount.of(1)));
        ds.register(delegate.toAddress(), Bytes.of("delegate"));

        TransactionType type = TransactionType.UNVOTE;
        byte[] from = voter.toAddress();
        byte[] to = delegate.toAddress();
        Amount value = Amount.of(100, SEM);
        Amount fee = config.spec().minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] data = {};

        // unvote (never voted before)
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data).sign(voter);

        TransactionResult result = exec.execute(tx, as.track(), ds.track(), block, 0);
        assertFalse(result.getCode().isSuccess());
        assertEquals(INSUFFICIENT_AVAILABLE, result.code);
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
