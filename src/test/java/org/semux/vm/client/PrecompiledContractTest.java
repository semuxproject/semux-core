/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.Unit.SEM;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.core.Amount;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.TransactionExecutor;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.TimeUtil;

public class PrecompiledContractTest {

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
        chain = spy(new BlockchainImpl(config, temporaryDBFactory));
        as = chain.getAccountState();
        ds = chain.getDelegateState();
        exec = new TransactionExecutor(config, new SemuxBlockStore(chain));
        network = config.network();

        doReturn(true).when(chain).isForkActivated(any(), anyLong());
    }

    // pragma solidity ^0.4.14;
    //
    // contract Vote {
    // function vote(address to, uint amount) {
    // require(0x0000000000000000000000000000000000000064.call(to, amount));
    // }
    //
    // function unvote(address to, uint amount) {
    // require(0x0000000000000000000000000000000000000065.call(to, amount));
    // }
    // }
    @Test
    public void testSuccess() {
        Key key = new Key();

        TransactionType type = TransactionType.CALL;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        byte[] delegate = Bytes.random(20);
        Amount value = NANO_SEM.of(0);
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();

        byte[] contract = Hex
                .decode("60806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806302aa9be2146100515780635f74bbde1461009e575b600080fd5b34801561005d57600080fd5b5061009c600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506100eb565b005b3480156100aa57600080fd5b506100e9600480360381019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610165565b005b606573ffffffffffffffffffffffffffffffffffffffff168282604051808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506000604051808303816000865af1915050151561016157600080fd5b5050565b606473ffffffffffffffffffffffffffffffffffffffff168282604051808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506000604051808303816000865af191505015156101db57600080fd5b50505600a165627a7a723058205e1325476a012c885717ccd0ad038a0d8d6c20f2e5afcb1b475d3eac5313c9500029");
        as.setCode(to, contract);

        SemuxBlock bh = new SemuxBlock(
                new BlockHeader(123l, Bytes.random(20), Bytes.random(20), System.currentTimeMillis(),
                        Bytes.random(20), Bytes.random(20), Bytes.random(20), Bytes.random(20)),
                config.spec().maxBlockGasLimit());
        as.adjustAvailable(from, SEM.of(1000));
        as.adjustAvailable(to, SEM.of(1000));
        ds.register(delegate, "abc".getBytes());

        byte[] data = Bytes.merge(Hex.decode("5f74bbde"),
                Hex.decode("000000000000000000000000"), delegate,
                Hex.decode("000000000000000000000000000000000000000000000000000000003B9ACA00")); // 1 nanoSEM
        long gas = 100000;
        long gasPrice = 1;

        Transaction tx = new Transaction(network, type, to, value, Amount.ZERO, nonce, timestamp, data, gas, gasPrice);
        tx.sign(key);

        long dataGasCost = 0;
        for (byte b : data) {
            dataGasCost += (b == 0 ? 4 : 68);
        }

        TransactionResult result = exec.execute(tx, as, ds, bh, chain);
        assertTrue(result.getCode().isSuccess());
        assertEquals(21_000 + 21_000 + dataGasCost + 1088, result.getGasUsed());
        assertEquals(SEM.of(1000).getNano() - result.getGasUsed() * gasPrice,
                as.getAccount(from).getAvailable().getNano());
        assertEquals(SEM.of(1000).getNano() - 1, as.getAccount(to).getAvailable().getNano());
        assertEquals(1, as.getAccount(to).getLocked().getNano());
        assertEquals(0, as.getAccount(delegate).getAvailable().getNano());
        assertEquals(0, as.getAccount(delegate).getLocked().getNano());

        data = Bytes.merge(Hex.decode("02aa9be2"),
                Hex.decode("000000000000000000000000"), delegate,
                Hex.decode("000000000000000000000000000000000000000000000000000000003B9ACA00")); // 1 nanoSEM

        tx = new Transaction(network, type, to, value, Amount.ZERO, nonce + 1, timestamp, data, gas, gasPrice);
        tx.sign(key);

        result = exec.execute(tx, as, ds, bh, chain);
        assertTrue(result.getCode().isSuccess());
        assertEquals(SEM.of(1000).getNano(), as.getAccount(to).getAvailable().getNano());
        assertEquals(0, as.getAccount(to).getLocked().getNano());
        assertEquals(0, as.getAccount(delegate).getAvailable().getNano());
        assertEquals(0, as.getAccount(delegate).getLocked().getNano());
    }
}
