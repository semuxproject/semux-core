/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

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
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

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
        exec = new TransactionExecutor(config, chain);
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
        byte[] contract = compile("PUSH1 0xa0");
        as.setCode(to, contract);

        BlockHeader bh = new BlockHeader(123l, Bytes.random(20), Bytes.random(20), System.currentTimeMillis(),
                Bytes.random(20), Bytes.random(20), Bytes.random(20), Bytes.random(20));

        byte[] data = Bytes.random(16);

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data, Amount.ZERO,
                Amount.ZERO);
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

    private TransactionResult executeAndCommit(TransactionExecutor exec, Transaction tx, AccountState as,
            DelegateState ds, BlockHeader bh) {
        TransactionResult res = exec.execute(tx, as, ds, bh);
        as.commit();
        ds.commit();

        return res;
    }
}
