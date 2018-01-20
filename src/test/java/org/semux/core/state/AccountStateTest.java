/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Genesis.Premine;
import org.semux.core.Unit;
import org.semux.rules.TemporaryDbRule;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

public class AccountStateTest {

    private Blockchain chain;
    private AccountState state;

    @Rule
    public TemporaryDbRule temporaryDBFactory = new TemporaryDbRule();

    @Before
    public void setUp() {
        chain = new BlockchainImpl(new DevnetConfig(Constants.DEFAULT_DATA_DIR), temporaryDBFactory);
        state = chain.getAccountState();
    }

    @Test
    public void testAtGenesis() {
        Map<ByteArray, Premine> premine = chain.getGenesis().getPremines();

        for (ByteArray k : premine.keySet()) {
            Account acc = state.getAccount(k.getData());
            assertEquals(premine.get(k).getAmount() * Unit.SEM, acc.getAvailable());
        }
    }

    @Test
    public void testAccount() {
        byte[] address = Bytes.random(20);
        Account acc = state.getAccount(address);
        acc.setAvailable(1);
        acc.setLocked(2);
        acc.setNonce(3);

        Account acc2 = Account.fromBytes(address, acc.toBytes());
        assertEquals(1L, acc2.getAvailable());
        assertEquals(2L, acc2.getLocked());
        assertEquals(3L, acc2.getNonce());
    }

    @Test
    public void testNonExists() {
        byte[] address = Bytes.random(20);
        Account acc = state.getAccount(address);

        assertArrayEquals(address, acc.getAddress());
        assertEquals(0, acc.getAvailable());
        assertEquals(0, acc.getLocked());
        assertEquals(0, acc.getNonce());
    }

    @Test
    public void testAvailable() {
        byte[] address = Bytes.random(20);
        assertEquals(0, state.getAccount(address).getAvailable());
        state.adjustAvailable(address, 20);
        assertEquals(20, state.getAccount(address).getAvailable());

        AccountState state2 = state.track();
        assertEquals(20, state2.getAccount(address).getAvailable());

        state.rollback();
        assertEquals(0, state2.getAccount(address).getAvailable());
    }

    @Test
    public void testLocked() {
        byte[] address = Bytes.random(20);
        assertEquals(0, state.getAccount(address).getLocked());
        state.adjustLocked(address, 20);
        assertEquals(20, state.getAccount(address).getLocked());

        AccountState state2 = state.track();
        assertEquals(20, state2.getAccount(address).getLocked());

        state.rollback();
        assertEquals(0, state2.getAccount(address).getLocked());
    }

    @Test
    public void testNonce() {
        byte[] address = Bytes.random(20);
        assertEquals(0, state.getAccount(address).getNonce());
        state.increaseNonce(address);
        assertEquals(1, state.getAccount(address).getNonce());

        AccountState state2 = state.track();
        assertEquals(1, state2.getAccount(address).getNonce());

        state.rollback();
        assertEquals(0, state2.getAccount(address).getNonce());
    }
}
