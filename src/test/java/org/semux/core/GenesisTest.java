/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.semux.core.Genesis.Premine;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.utils.ByteArray;

public class GenesisTest {

    private static byte[] ZERO_ADDRESS = Hex.decode("0000000000000000000000000000000000000000");
    private static byte[] ZERO_HASH = Hash.EMPTY_H256;

    Genesis genesis = Genesis.getInstance();

    @Test
    public void testIsGenesis() {
        Genesis genesis = Genesis.getInstance();

        assertTrue(genesis.isGensis());
    }

    @Test
    public void testBlock() {
        Genesis genesis = Genesis.getInstance();

        assertTrue(genesis.getNumber() == 0);
        assertArrayEquals(ZERO_ADDRESS, genesis.getCoinbase());
        assertArrayEquals(ZERO_HASH, genesis.getPrevHash());
        assertTrue(genesis.getTimestamp() > 0);
        assertFalse(Arrays.equals(ZERO_ADDRESS, genesis.getHash()));
    }

    @Test
    public void testPremines() {
        Genesis genesis = Genesis.getInstance();
        Map<ByteArray, Premine> premine = genesis.getPremines();

        assertFalse(premine.isEmpty());
        for (Premine p : premine.values()) {
            assertTrue(p.getAmount() > 0);
        }
    }

    @Test
    public void testDelegates() {
        Genesis genesis = Genesis.getInstance();
        Map<String, byte[]> delegates = genesis.getDelegates();

        assertFalse(delegates.isEmpty());
        for (byte[] addr : delegates.values()) {
            assertTrue(addr.length == 20);
        }
    }

    @Test
    public void testConfig() {
        Genesis genesis = Genesis.getInstance();

        assertTrue(genesis.getConfig().isEmpty());
    }
}
