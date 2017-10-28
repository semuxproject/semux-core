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

import org.junit.Test;
import org.semux.core.Account;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Genesis.Premine;
import org.semux.db.MemoryDB;
import org.semux.utils.ByteArray;
import org.semux.utils.Bytes;

public class AccountStateTest {

    @Test
    public void testAtGenesis() {
        Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
        AccountState state = chain.getAccountState();

        Map<ByteArray, Premine> premine = chain.getGenesis().getPremines();

        for (ByteArray k : premine.keySet()) {
            Account acc = state.getAccount(k.getData());
            assertEquals((long) premine.get(k).getAmount(), acc.getBalance());
        }
    }

    @Test
    public void testAccount() {
        Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
        AccountState state = chain.getAccountState();

        byte[] addr = Bytes.random(20);
        Account acc = state.getAccount(addr);
        acc.setBalance(1);
        acc.setLocked(2);
        acc.setNonce(3);
        acc.setCode(Bytes.of("test"));
        acc.putStorage(Bytes.of("key"), Bytes.of("value"));
        state.commit();

        Account acc2 = state.getAccount(addr);
        assertEquals(1L, acc2.getBalance());
        assertEquals(2L, acc2.getLocked());
        assertEquals(3L, acc2.getNonce());
        assertArrayEquals(Bytes.of("test"), acc2.getCode());
        assertArrayEquals(Bytes.of("value"), acc2.getStorage(Bytes.of("key")));
    }
}
