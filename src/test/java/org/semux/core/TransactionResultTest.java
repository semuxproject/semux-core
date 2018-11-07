/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.junit.Test;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionResultTest {

    private static final Logger logger = LoggerFactory.getLogger(TransactionResultTest.class);

    private boolean valid = true;
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
    public void testNew() {
        TransactionResult res = new TransactionResult(valid, returns, logs, 0);
        assertTrue(res.validate());

        testFields(res);
    }

    @Test
    public void testSerialization() {
        TransactionResult res = new TransactionResult(valid, returns, logs, 0);

        testFields(TransactionResult.fromBytes(res.toBytes()));
    }

    @Test
    public void testTransactionResultSize() {
        TransactionResult res = new TransactionResult(valid, returns, logs, 0);
        byte[] bytes = res.toBytes();

        logger.info("result size: {} B, {} GB per 1M txs", bytes.length, 1000000.0 * bytes.length / 1024 / 1024 / 1024);
    }

    private void testFields(TransactionResult res) {
        assertEquals(valid, res.isSuccess());
        assertArrayEquals(returns, res.getReturns());
        assertEquals(logs.size(), res.getLogs().size());
        for (int i = 0; i < logs.size(); i++) {
            assertEquals(logs.get(i).toString(), res.getLogs().get(i).toString());
        }
    }
}
