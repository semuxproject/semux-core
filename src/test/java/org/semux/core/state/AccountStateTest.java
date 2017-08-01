/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.semux.core.Account;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.db.MemoryDB;
import org.semux.utils.ByteArray;

public class AccountStateTest {

    @Test
    public void testAtGenesis() {
        Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
        AccountState state = chain.getAccountState();

        Map<ByteArray, Long> premine = chain.getGenesis().getPremine();

        for (ByteArray k : premine.keySet()) {
            Account acc = state.getAccount(k.getData());
            assertEquals((long) premine.get(k), acc.getBalance());
        }
    }
}
