/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.ethereum.vm.util.HashUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;
import org.semux.vm.client.SemuxBlockStore;

import static org.ethereum.vm.util.BytecodeCompiler.compile;
import static org.junit.Assert.*;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.Amount.*;

public class VmTest {

    @Rule
    public TemporaryDatabaseRule temporaryDBFactory = new TemporaryDatabaseRule();

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
        exec = new TransactionExecutor(config, new SemuxBlockStore(chain));
        network = config.network();
    }

    /**
     * Just a basic test to check wiring so far
     */
    @Test
    public void testCall() {
        Key key = new Key();

        TransactionType type = TransactionType.CALL;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        Amount value = NANO_SEM.of(5);
        Amount fee = config.minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();

        // set the contract to a simple program
        byte[] contract = compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x00 RETURN");
        System.out.println(Hex.encode0x(contract));
        System.out.println(
                Hex.encode0x(HashUtil.calcNewAddress(Hex.decode0x("0x23a6049381fd2cfb0661d9de206613b83d53d7df"), 17)));
        as.setCode(to, contract);

        BlockHeader bh = new BlockHeader(123l, Bytes.random(20), Bytes.random(20), System.currentTimeMillis(),
                Bytes.random(20), Bytes.random(20), Bytes.random(20), Bytes.random(20));

        byte[] data = Bytes.random(16);
        long gas = 30000;
        long gasPrice = 1;

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data, gas, gasPrice);
        tx.sign(key);
        assertTrue(tx.validate(network));

        // insufficient available
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), bh);
        assertFalse(result.isSuccess());

        Amount available = SEM.of(1000);
        as.adjustAvailable(key.toAddress(), available);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track(), bh);
        assertTrue(result.isSuccess());
        assertEquals(available, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(ZERO, as.getAccount(to).getAvailable());

        // execute and commit
        result = executeAndCommit(exec, tx, as.track(), ds.track(), bh);
        assertTrue(result.isSuccess());
    }

    /**
     * Just a basic test to check wiring so far
     */
    @Test
    public void testCreate() {
        Key key = new Key();

        TransactionType type = TransactionType.CREATE;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        Amount value = NANO_SEM.of(5);
        Amount fee = config.minTransactionFee();
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();

        // set the contract to a simple program
        byte[] data = compile("PUSH1 0xa0");

        BlockHeader bh = new BlockHeader(123l, Bytes.random(20), Bytes.random(20), System.currentTimeMillis(),
                Bytes.random(20), Bytes.random(20), Bytes.random(20), Bytes.random(20));

        long gas = 60000;
        long gasPrice = 1;

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data, gas, gasPrice);
        tx.sign(key);
        assertTrue(tx.validate(network));

        // insufficient available
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), bh);
        assertFalse(result.isSuccess());

        Amount available = SEM.of(1000);
        as.adjustAvailable(key.toAddress(), available);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track(), bh);
        assertTrue(result.isSuccess());
        assertEquals(available, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(ZERO, as.getAccount(to).getAvailable());

        // execute and commit
        result = executeAndCommit(exec, tx, as.track(), ds.track(), bh);
        assertTrue(result.isSuccess());

        byte[] newContractAddress = HashUtil.calcNewAddress(tx.getFrom(), tx.getNonce());

        byte[] contract = as.getCode(newContractAddress);
        assertArrayEquals(data, contract);
    }

    private TransactionResult executeAndCommit(TransactionExecutor exec, Transaction tx, AccountState as,
            DelegateState ds, BlockHeader bh) {
        TransactionResult res = exec.execute(tx, as, ds, bh);
        as.commit();
        ds.commit();

        return res;
    }
}
