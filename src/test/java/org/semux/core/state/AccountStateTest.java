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
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

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
    public void testAvailable() {
        byte[] addr = Bytes.random(20);
        assertEquals(0, state.getAccount(addr).getAvailable());
        state.adjustAvailable(addr, 20);
        assertEquals(20, state.getAccount(addr).getAvailable());

        AccountState state2 = state.track();
        assertEquals(20, state2.getAccount(addr).getAvailable());

        state.rollback();
        assertEquals(0, state2.getAccount(addr).getAvailable());
    }

    @Test
    public void testLocked() {
        byte[] addr = Bytes.random(20);
        assertEquals(0, state.getAccount(addr).getLocked());
        state.adjustLocked(addr, 20);
        assertEquals(20, state.getAccount(addr).getLocked());

        AccountState state2 = state.track();
        assertEquals(20, state2.getAccount(addr).getLocked());

        state.rollback();
        assertEquals(0, state2.getAccount(addr).getLocked());
    }

    @Test
    public void testNonce() {
        byte[] addr = Bytes.random(20);
        assertEquals(0, state.getAccount(addr).getNonce());
        state.increaseNonce(addr);
        assertEquals(1, state.getAccount(addr).getNonce());

        AccountState state2 = state.track();
        assertEquals(1, state2.getAccount(addr).getNonce());

        state.rollback();
        assertEquals(0, state2.getAccount(addr).getNonce());
    }
}
