/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.crypto.EdDSA;
import org.semux.db.MemoryDB;
import org.semux.net.ChannelManager;

public class PendingManagerTest {

    private static Blockchain chain;
    private static ChannelManager channelMgr;
    private static PendingManager pm;

    private static TransactionType type = TransactionType.TRANSFER;
    private static EdDSA key = new EdDSA();

    @BeforeClass
    public static void setup() {
        chain = new BlockchainImpl(MemoryDB.FACTORY);
        channelMgr = new ChannelManager();

        pm = PendingManager.getInstance();
        pm.start(chain, channelMgr);
    }

    @Before
    public void clear() {
        pm.removeTransactions(pm.getTransactions());
    }

    @Test
    public void testGetTransaction() throws InterruptedException {
        long now = System.currentTimeMillis();
        assertEquals(0, pm.getTransactions().size());

        Transaction tx = new Transaction(type, new byte[20], new byte[20], 0, 0, 1, now, new byte[32]).sign(key);
        pm.addTransaction(tx);
        Thread.sleep(100);

        assertEquals(1, pm.getTransactions().size());
    }

    @Test
    public void testAddTransaction() throws InterruptedException {
        long now = System.currentTimeMillis();

        Transaction tx = new Transaction(type, new byte[20], new byte[20], 0, 0, 1, now, new byte[32]).sign(key);
        pm.addTransaction(tx);
        Transaction tx2 = new Transaction(type, new byte[20], new byte[20], 0, 0, 1, now, new byte[5 * 1024]).sign(key);
        pm.addTransaction(tx2);
        Thread.sleep(100);

        assertEquals(1, pm.getTransactions().size());
    }

    @Test
    public void testRemoveTransactions() throws InterruptedException {
        long now = System.currentTimeMillis();

        Transaction tx = new Transaction(type, new byte[20], new byte[20], 0, 0, 1, now + 1, new byte[32]).sign(key);
        pm.addTransaction(tx);
        Thread.sleep(100);

        assertEquals(1, pm.getTransactions().size());
        pm.removeTransaction(tx);
        assertEquals(0, pm.getTransactions().size());
    }

    @AfterClass
    public static void teardown() {
        pm.stop();
    }
}
