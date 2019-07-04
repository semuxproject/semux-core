/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import static org.ethereum.vm.util.BytecodeCompiler.compile;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.Amount.ZERO;

import org.ethereum.vm.util.HashUtil;
import org.ethereum.vm.util.HexUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VmTransactionTest {

    private Logger logger = LoggerFactory.getLogger(VmTransactionTest.class);

    @Rule
    public TemporaryDatabaseRule temporaryDBFactory = new TemporaryDatabaseRule();

    private Config config;
    private Blockchain chain;
    private AccountState as;
    private DelegateState ds;
    private TransactionExecutor exec;
    private Network network;

    private SemuxBlock block;

    @Before
    public void prepare() {
        config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);
        chain = Mockito.spy(new BlockchainImpl(config, temporaryDBFactory));
        as = chain.getAccountState();
        ds = chain.getDelegateState();
        exec = new TransactionExecutor(config, new SemuxBlockStore(chain));
        network = config.network();

        doReturn(true).when(chain).isForkActivated(any(), anyLong());

        block = new SemuxBlock(
                new BlockHeader(1l, Bytes.random(20), Bytes.random(32), System.currentTimeMillis(),
                        Bytes.random(20), Bytes.random(20), Bytes.random(20), Bytes.random(20)),
                config.spec().maxBlockGasLimit());
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
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();

        // set the contract to a simple program
        byte[] contract = compile("PUSH2 0x1234 PUSH1 0x00 MSTORE PUSH1 0x20 PUSH1 0x00 RETURN");
        logger.info(Hex.encode0x(contract));
        logger.info(
                Hex.encode0x(HashUtil.calcNewAddress(Hex.decode0x("0x23a6049381fd2cfb0661d9de206613b83d53d7df"), 17)));
        as.setCode(to, contract);

        byte[] data = Bytes.random(16);
        long gas = 30000;
        long gasPrice = 1;

        Transaction tx = new Transaction(network, type, to, value, Amount.ZERO, nonce, timestamp, data, gas, gasPrice);
        tx.sign(key);
        assertTrue(tx.validate(network));

        // insufficient available
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), block, chain);
        assertFalse(result.getCode().isSuccess());

        Amount available = SEM.of(1000);
        as.adjustAvailable(key.toAddress(), available);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track(), block, chain);
        assertTrue(result.getCode().isSuccess());
        assertEquals(available, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(ZERO, as.getAccount(to).getAvailable());

        // execute and commit
        result = exec.execute(tx, as, ds, block, chain);
        assertTrue(result.getCode().isSuccess());
    }

    /**
     * Just a basic test to check wiring so far
     */
    @Test
    public void testCreate() {
        Key key = new Key();

        TransactionType type = TransactionType.CREATE;
        byte[] from = key.toAddress();
        byte[] to = Bytes.EMPTY_ADDRESS;
        Amount value = NANO_SEM.of(0);
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();

        // set the contract to a simple program
        String code = "608060405234801561001057600080fd5b506040516020806100e7833981018060405281019080805190602001909291905050508060008190555050609e806100496000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632e52d606146044575b600080fd5b348015604f57600080fd5b506056606c565b6040518082815260200191505060405180910390f35b600054815600a165627a7a72305820efb6a6369e3c5d7fe9b3274b20753bb0fe188b763fc2adee86cd844de935c8220029";
        byte[] create = HexUtil.fromHexString(code);
        byte[] data = HexUtil.fromHexString(code.substring(code.indexOf("60806040", 1)));

        long gas = 1000000;
        long gasPrice = 1;

        Transaction tx = new Transaction(network, type, to, value, Amount.ZERO, nonce, timestamp, create, gas,
                gasPrice);
        tx.sign(key);
        assertTrue(tx.validate(network));

        // insufficient available
        TransactionResult result = exec.execute(tx, as.track(), ds.track(), block, chain);
        assertFalse(result.getCode().isSuccess());

        Amount available = SEM.of(1000);
        as.adjustAvailable(key.toAddress(), available);

        // execute but not commit
        result = exec.execute(tx, as.track(), ds.track(), block, chain);
        assertTrue(result.getCode().isSuccess());
        assertEquals(available, as.getAccount(key.toAddress()).getAvailable());
        assertEquals(ZERO, as.getAccount(to).getAvailable());

        // execute and commit
        result = exec.execute(tx, as, ds, block, chain);
        assertTrue(result.getCode().isSuccess());

        byte[] newContractAddress = HashUtil.calcNewAddress(tx.getFrom(), tx.getNonce());

        byte[] contract = as.getCode(newContractAddress);
        assertArrayEquals(data, contract);
    }

    @Test
    public void testTransferToContract() {
        Key key = new Key();

        TransactionType type = TransactionType.CALL;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        Amount value = NANO_SEM.of(50);
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();

        byte[] contract = Hex.decode("60006000");
        as.setCode(to, contract);
        as.adjustAvailable(from, SEM.of(1000));

        byte[] data = contract;
        long gas = 100000;
        long gasPrice = 1;

        Transaction tx = new Transaction(network, type, to, value, Amount.ZERO, nonce, timestamp, data, gas, gasPrice);
        tx.sign(key);

        TransactionResult result = exec.execute(tx, as, ds, block, chain);
        assertTrue(result.getCode().isSuccess());
        assertEquals(SEM.of(1000).getNano() - value.getNano() - tx.getGasPrice() * result.getGasUsed(),
                as.getAccount(from).getAvailable().getNano());
        assertEquals(value.getNano(), as.getAccount(to).getAvailable().getNano());
    }

    @Test
    public void testTransferToContractOutOfGas() {
        Key key = new Key();

        TransactionType type = TransactionType.CALL;
        byte[] from = key.toAddress();
        byte[] to = Bytes.random(20);
        Amount value = NANO_SEM.of(50);
        long nonce = as.getAccount(from).getNonce();
        long timestamp = TimeUtil.currentTimeMillis();

        byte[] contract = Hex.decode("6000");
        as.setCode(to, contract);
        as.adjustAvailable(from, SEM.of(1000));

        byte[] data = contract;
        long gas = 21073;
        long gasPrice = 1;

        Transaction tx = new Transaction(network, type, to, value, Amount.ZERO, nonce, timestamp, data, gas, gasPrice);
        tx.sign(key);

        TransactionResult result = exec.execute(tx, as, ds, block, chain);
        assertTrue(result.getCode().isFailure());
        assertEquals(SEM.of(1000).getNano() - tx.getGasPrice() * result.getGasUsed(),
                as.getAccount(from).getAvailable().getNano());
        assertEquals(0, as.getAccount(to).getAvailable().getNano());
        // value transfer reverted
    }
}
