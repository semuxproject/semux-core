/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.core.Genesis.Premine;
import org.semux.crypto.Hex;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

public class GenesisTest {

    private static byte[] ZERO_ADDRESS = Hex.decode("0000000000000000000000000000000000000000");
    private static byte[] ZERO_HASH = Bytes.EMPTY_HASH;

    Genesis genesis;

    @Before
    public void setup() {
        genesis = Genesis.load(new File(Constants.DEFAULT_DATA_DIR));
    }

    @Test
    public void testIsGenesis() {
        assertTrue(genesis.getNumber() == 0);
    }

    @Test
    public void testBlock() {
        assertTrue(genesis.getNumber() == 0);
        assertArrayEquals(ZERO_ADDRESS, genesis.getCoinbase());
        assertArrayEquals(ZERO_HASH, genesis.getPrevHash());
        assertTrue(genesis.getTimestamp() > 0);
        assertFalse(Arrays.equals(ZERO_ADDRESS, genesis.getHash()));
    }

    @Test
    public void testPremines() {
        Map<ByteArray, Premine> premine = genesis.getPremines();

        assertFalse(premine.isEmpty());
        for (Premine p : premine.values()) {
            assertTrue(p.getAmount() > 0);
        }
    }

    @Test
    public void testDelegates() {
        Map<String, byte[]> delegates = genesis.getDelegates();

        assertFalse(delegates.isEmpty());
        for (byte[] addr : delegates.values()) {
            assertTrue(addr.length == 20);
        }
    }

    @Test
    public void testConfig() {
        assertTrue(genesis.getConfig().isEmpty());
    }
}
