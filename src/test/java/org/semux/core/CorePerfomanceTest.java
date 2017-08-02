/*
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

import org.junit.Test;
import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;
import org.semux.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorePerfomanceTest {

    private static final Logger logger = LoggerFactory.getLogger(CorePerfomanceTest.class);

    @Test
    public void testSortDelegate() {
        List<Delegate> list = new ArrayList<>();
        int nDelegates = 100_000;

        Random r = new Random();
        for (int i = 0; i < nDelegates; i++) {
            Delegate d = new Delegate(Bytes.random(20), Bytes.random(16), r.nextLong());
            list.add(d);
        }

        long t1 = System.nanoTime();
        list.sort((d1, d2) -> Long.compare(d2.getVote(), d1.getVote()));
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
            byte[] from = key.toAddress();
            byte[] to = Bytes.random(20);
            long value = 5;
            long fee = Config.MIN_TRANSACTION_FEE;
            long nonce = 1;
            long timestamp = System.currentTimeMillis();
            byte[] data = Bytes.random(16);

            Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
            tx.sign(key);
            txs.add(tx);
        }

        long t1 = System.nanoTime();
        for (Transaction tx : txs) {
            assertTrue(tx.validate());
        }
        long t2 = System.nanoTime();
        logger.info("Perf_transaction_1: {} μs/tx", (t2 - t1) / 1_000 / repeat);

        Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
        TransactionExecutor exec = TransactionExecutor.getInstance();

        t1 = System.nanoTime();
        exec.execute(txs, chain.getAccountState().track(), chain.getDeleteState().track(), true);
        t2 = System.nanoTime();
        logger.info("Perf_transaction_2: {} μs/tx", (t2 - t1) / 1_000 / repeat);
    }
}
