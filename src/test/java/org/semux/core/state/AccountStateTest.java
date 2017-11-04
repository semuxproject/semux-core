/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Genesis.Premine;
import org.semux.db.MemoryDB;
import org.semux.utils.ByteArray;
import org.semux.utils.Bytes;

public class AccountStateTest {

    private Blockchain chain;
    private AccountState state;

    @Before
    public void setup() {
        chain = new BlockchainImpl(MemoryDB.FACTORY);
        state = chain.getAccountState();
    }

    @Test
    public void testAtGenesis() {
        Map<ByteArray, Premine> premine = chain.getGenesis().getPremines();

        for (ByteArray k : premine.keySet()) {
            Account acc = state.getAccount(k.getData());
            assertEquals((long) premine.get(k).getAmount(), acc.getAvailable());
        }
    }

    @Test
    public void testAccount() {
        byte[] addr = Bytes.random(20);
        Account acc = state.getAccount(addr);
        acc.setAvailable(1);
        acc.setLocked(2);
        acc.setNonce(3);

        Account acc2 = Account.fromBytes(addr, acc.toBytes());
        assertEquals(1L, acc2.getAvailable());
        assertEquals(2L, acc2.getLocked());
        assertEquals(3L, acc2.getNonce());
    }

    @Test
    public void testNonExists() {
        byte[] addr = Bytes.random(20);
        Account acc = state.getAccount(addr);

        assertArrayEquals(addr, acc.getAddress());
        assertEquals(0, acc.getAvailable());
        assertEquals(0, acc.getLocked());
        assertEquals(0, acc.getNonce());
    }

    @Test
    public void testAdjustAvailable() {

    }
}
