/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConfigTest {

    @Test
    public void testLoad() {
        assertTrue(Config.init());
        assertEquals(1, Config.NETWORK_ID);
    }

    @Test
    public void testNumberOfValidators() {
        assertEquals(10, Config.getNumberOfValidators(0));
        assertEquals(10, Config.getNumberOfValidators(1));
        assertEquals(20, Config.getNumberOfValidators(30_000));
        assertEquals(30, Config.getNumberOfValidators(60_000));
        assertEquals(100, Config.getNumberOfValidators(270_000));
        assertEquals(100, Config.getNumberOfValidators(300_000));
        assertEquals(100, Config.getNumberOfValidators(Long.MAX_VALUE));
    }
}
