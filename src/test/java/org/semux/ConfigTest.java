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
    }

    @Test
    public void testNumberOfValidators() {
        long step = 2 * 60 * 9;

        assertEquals(20, Config.getNumberOfValidators(0));
        assertEquals(20, Config.getNumberOfValidators(1));
        assertEquals(21, Config.getNumberOfValidators(step));
        assertEquals(22, Config.getNumberOfValidators(2 * step));
        assertEquals(70, Config.getNumberOfValidators(50 * step));
        assertEquals(99, Config.getNumberOfValidators(80 * step - 1));
        assertEquals(100, Config.getNumberOfValidators(80 * step));
        assertEquals(100, Config.getNumberOfValidators(100 * step));
        assertEquals(100, Config.getNumberOfValidators(Long.MAX_VALUE));
    }
}
