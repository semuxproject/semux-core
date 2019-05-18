/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.ZERO;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.Network;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainFactory;
import org.semux.core.Genesis;
import org.semux.core.Genesis.Premine;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

public class AccountStateV2Test {

    private Blockchain chain;
    private AccountState state;

    @Rule
    public TemporaryDatabaseRule temporaryDBFactory = new TemporaryDatabaseRule();

    @Before
    public void setUp() {
        chain = (new BlockchainFactory(new DevnetConfig(Constants.DEFAULT_DATA_DIR), Genesis.load(Network.DEVNET),
                temporaryDBFactory)).getBlockchainInstance();
        state = chain.getAccountState();
    }

    @Test
    public void testAtGenesis() {
        Map<ByteArray, Premine> premine = chain.getGenesis().getPremines();

        for (ByteArray k : premine.keySet()) {
            Account acc = state.getAccount(k.getData());
            assertEquals(premine.get(k).getAmount(), acc.getAvailable());
        }
    }

    @Test
    public void testAccount() {
        byte[] address = Bytes.random(20);
        Account acc = state.getAccount(address);
        acc.setAvailable(NANO_SEM.of(1));
        acc.setLocked(NANO_SEM.of(2));
        acc.setNonce(3);

        Account acc2 = Account.fromBytes(address, acc.toBytes());
        assertEquals(NANO_SEM.of(1), acc2.getAvailable());
        assertEquals(NANO_SEM.of(2), acc2.getLocked());
        assertEquals(3L, acc2.getNonce());
    }

    @Test
    public void testNonExists() {
        byte[] address = Bytes.random(20);
        Account acc = state.getAccount(address);

        assertArrayEquals(address, acc.getAddress());
        assertEquals(ZERO, acc.getAvailable());
        assertEquals(ZERO, acc.getLocked());
        assertEquals(0, acc.getNonce());
    }

    @Test
    public void testCode() {
        byte[] address = Bytes.random(20);
        byte[] addressCode = Bytes.random(20);
        byte[] code = state.getCode(address);
        assertNull(code);
        state.setCode(address, addressCode);
        code = state.getCode(address);
        assertArrayEquals(addressCode, code);
        commit();
        code = state.getCode(address);
        assertArrayEquals(addressCode, code);
    }

    @Test
    public void testStorage() {
        byte[] address = Bytes.random(20);
        byte[] addressStorage1 = Bytes.random(20);
        byte[] storageKey1 = Bytes.random(3);
        byte[] storageKey2 = Bytes.random(3);
        byte[] addressStorage2 = Bytes.random(21);
        state.putStorage(address, storageKey2, addressStorage2);
        byte[] storage = state.getStorage(address, storageKey1);
        assertNull(storage);
        state.putStorage(address, storageKey1, addressStorage1);
        storage = state.getStorage(address, storageKey1);
        assertArrayEquals(addressStorage1, storage);
        commit();
        storage = state.getStorage(address, storageKey1);
        assertArrayEquals(addressStorage1, storage);
    }

    @Test
    public void testAvailable() {
        byte[] address = Bytes.random(20);
        assertEquals(ZERO, state.getAccount(address).getAvailable());
        state.adjustAvailable(address, NANO_SEM.of(20));
        assertEquals(NANO_SEM.of(20), state.getAccount(address).getAvailable());

        AccountState state2 = state.track();
        assertEquals(NANO_SEM.of(20), state2.getAccount(address).getAvailable());

        state.rollback();
        assertEquals(ZERO, state2.getAccount(address).getAvailable());
    }

    @Test
    public void testLocked() {
        byte[] address = Bytes.random(20);
        assertEquals(ZERO, state.getAccount(address).getLocked());
        state.adjustLocked(address, NANO_SEM.of(20));
        assertEquals(NANO_SEM.of(20), state.getAccount(address).getLocked());

        AccountState state2 = state.track();
        assertEquals(NANO_SEM.of(20), state2.getAccount(address).getLocked());

        state.rollback();
        assertEquals(ZERO, state2.getAccount(address).getLocked());
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

    private void commit() {
        state.commit();
        chain.commit();
    }
}
