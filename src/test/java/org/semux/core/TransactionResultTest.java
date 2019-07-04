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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.OpCode;
import org.junit.Test;
import org.semux.util.Bytes;
import org.semux.vm.client.SemuxInternalTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionResultTest {

    private static final Logger logger = LoggerFactory.getLogger(TransactionResultTest.class);

    private byte[] returns = Bytes.random(20);
    private List<LogInfo> logs = new ArrayList<>();

    public TransactionResultTest() {

        logs.add(createLogInfo());
        logs.add(createLogInfo());
    }

    private LogInfo createLogInfo() {
        List<DataWord> topics = new ArrayList<>();
        topics.add(DataWord.of(Bytes.random(8)));
        topics.add(DataWord.of(1980));

        return new LogInfo(Bytes.random(22), topics, Bytes.random(8));
    }

    @Test
    public void testCode() {
        for (TransactionResult.Code code : TransactionResult.Code.values()) {
            if (code.name().startsWith("SUCCESS")) {
                assertTrue(code.isSuccess());
                assertFalse(code.isFailure());
                assertTrue(code.isAcceptable());
                assertFalse(code.isRejected());
            } else if (code.name().startsWith("FAILURE")) {
                assertFalse(code.isSuccess());
                assertTrue(code.isFailure());
                assertTrue(code.isAcceptable());
                assertFalse(code.isRejected());
            } else {
                assertFalse(code.isSuccess());
                assertFalse(code.isFailure());
                assertFalse(code.isAcceptable());
                assertTrue(code.isRejected());
            }
        }
    }

    @Test
    public void testNew() {
        TransactionResult res = new TransactionResult(TransactionResult.Code.SUCCESS, returns, logs);
        testFields(res);
    }

    @Test
    public void testSerialization() {

        TransactionResult res = new TransactionResult(TransactionResult.Code.SUCCESS, returns, logs);

        testFields(TransactionResult.fromBytes(res.toBytes()));
    }

    @Test
    public void testTransactionResultSize() {
        TransactionResult res = new TransactionResult(TransactionResult.Code.SUCCESS, returns, logs);
        byte[] bytes = res.toBytes();

        logger.info("result size: {} B, {} GB per 1M txs", bytes.length, 1000000.0 * bytes.length / 1024 / 1024 / 1024);
    }

    private void testFields(TransactionResult res) {
        assertArrayEquals(returns, res.getReturnData());
        assertEquals(logs.size(), res.getLogs().size());
        for (int i = 0; i < logs.size(); i++) {
            assertEquals(logs.get(i).toString(), res.getLogs().get(i).toString());
        }
    }

    @Test
    public void testSerializationFull() {
        TransactionResult.Code code = TransactionResult.Code.FAILURE;
        byte[] returnData = Bytes.random(128);
        List<LogInfo> logs = new ArrayList<>();
        logs.add(new LogInfo(Bytes.random(20), Arrays.asList(DataWord.ONE, DataWord.ZERO), Bytes.random(48)));
        long gas = 1;
        long gasPrice = 2;
        long gasUsed = 3;
        long blockNumber = 4;
        List<SemuxInternalTransaction> internalTransactions = new ArrayList<>();
        internalTransactions
                .add(new SemuxInternalTransaction(false, 1, 2, OpCode.CALL, Bytes.random(20), Bytes.random(20),
                        3, Amount.Unit.NANO_SEM.of(1), Bytes.random(5), 4, Amount.Unit.NANO_SEM.of(10)));

        TransactionResult tr1 = new TransactionResult(code, returnData, logs);
        tr1.setGas(gas, gasPrice, gasUsed);
        tr1.setBlockNumber(blockNumber);
        tr1.setInternalTransactions(internalTransactions);

        TransactionResult tr2 = TransactionResult.fromBytes(tr1.toBytes());
        assertEquals(code, tr2.getCode());
        assertArrayEquals(returnData, tr2.getReturnData());
        assertEquals(logs.size(), tr2.getLogs().size());
        assertArrayEquals(logs.get(0).getAddress(), tr2.getLogs().get(0).getAddress());
        assertEquals(logs.get(0).getTopics().get(1), tr2.getLogs().get(0).getTopics().get(1));
        assertArrayEquals(logs.get(0).getData(), tr2.getLogs().get(0).getData());
        assertEquals(gas, tr2.getGas());
        assertEquals(gasPrice, tr2.getGasPrice());
        assertEquals(gasUsed, tr2.getGasUsed());
        assertEquals(blockNumber, tr2.getBlockNumber());
        assertEquals(internalTransactions.size(), tr2.getInternalTransactions().size());
        assertEquals(internalTransactions.get(0).getDepth(), tr2.getInternalTransactions().get(0).getDepth());
        assertEquals(internalTransactions.get(0).getIndex(), tr2.getInternalTransactions().get(0).getIndex());
        assertEquals(internalTransactions.get(0).getType(), tr2.getInternalTransactions().get(0).getType());
        assertArrayEquals(internalTransactions.get(0).getFrom(), tr2.getInternalTransactions().get(0).getFrom());
        assertArrayEquals(internalTransactions.get(0).getTo(), tr2.getInternalTransactions().get(0).getTo());
        assertEquals(internalTransactions.get(0).getNonce(), tr2.getInternalTransactions().get(0).getNonce());
        assertEquals(internalTransactions.get(0).getValue(), tr2.getInternalTransactions().get(0).getValue());
        assertArrayEquals(internalTransactions.get(0).getData(), tr2.getInternalTransactions().get(0).getData());
        assertEquals(internalTransactions.get(0).getGas(), tr2.getInternalTransactions().get(0).getGas());
        assertEquals(internalTransactions.get(0).getGasPrice(), tr2.getInternalTransactions().get(0).getGasPrice());
    }
}
