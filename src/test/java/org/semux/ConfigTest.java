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
import org.semux.core.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTest {

    private Logger logger = LoggerFactory.getLogger(ConfigTest.class);

    @Test
    public void testLoad() {
        assertTrue(Config.init());
    }

    @Test
    public void testBlockReward() {
        assertEquals(0, Config.getBlockReward(0));
        assertEquals(0, Config.getBlockReward(Config.DAY * 30));
        assertEquals(0, Config.getBlockReward(Config.DAY * 90));
        assertEquals(5 * Unit.SEM, Config.getBlockReward(Config.DAY * 180));
        assertEquals(5 * Unit.SEM, Config.getBlockReward(Config.DAY * 360));
        assertEquals(5 * Unit.SEM, Config.getBlockReward(Config.DAY * 720));
        assertEquals(0, Config.getBlockReward(Config.DAY * 365 * 15));
    }

    @Test
    public void testNumberOfValidators() {
        int last = 0;
        for (int i = 0; i < 60 * Config.DAY; i++) {
            int n = Config.getNumberOfValidators(i);
            if (n != last) {
                assertTrue(n > last);
                logger.info("block = {}, validators = {}", i, n);
                last = n;
            }
        }

        assertEquals(64, last);
    }
}
