/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.core.state.Delegate;
import org.semux.crypto.EdDSA;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorePerformanceTest {

    @Rule
    public TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    private static final Logger logger = LoggerFactory.getLogger(CorePerformanceTest.class);

    private Config config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);

    @Test
    public void testSortDelegate() {
        List<Delegate> list = new ArrayList<>();
        int nDelegates = 100_000;

        Random r = new Random();
        for (int i = 0; i < nDelegates; i++) {
            Delegate d = new Delegate(Bytes.random(20), Bytes.random(16), r.nextLong(), r.nextLong());
            list.add(d);
        }

        long t1 = System.nanoTime();
        list.sort((d1, d2) -> Long.compare(d2.getVotes(), d1.getVotes()));
        long t2 = System.nanoTime();
        logger.info("Perf_delegate_sort: {} μs", (t2 - t1) / 1_000);
    }

    @Test
    public void testTransactionProcessing() {
        List<Transaction> txs = new ArrayList<>();
        int repeat = 1000;

        for (int i = 0; i < repeat; i++) {
            EdDSA key = new EdDSA();

            TransactionType type = TransactionType.TRANSFER;
            byte[] to = Bytes.random(20);
            long value = 5;
            long fee = config.minTransactionFee();
            long nonce = 1;
            long timestamp = System.currentTimeMillis();
            byte[] data = Bytes.random(16);

            Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, data);
            tx.sign(key);
            txs.add(tx);
        }

        long t1 = System.nanoTime();
        for (Transaction tx : txs) {
            assertTrue(tx.validate());
        }
        long t2 = System.nanoTime();
        logger.info("Perf_transaction_1: {} μs/tx", (t2 - t1) / 1_000 / repeat);

        Blockchain chain = new BlockchainImpl(config, temporaryDBFactory);
        TransactionExecutor exec = new TransactionExecutor(config);

        t1 = System.nanoTime();
        exec.execute(txs, chain.getAccountState().track(), chain.getDelegateState().track());
        t2 = System.nanoTime();
        logger.info("Perf_transaction_2: {} μs/tx", (t2 - t1) / 1_000 / repeat);
    }
}
