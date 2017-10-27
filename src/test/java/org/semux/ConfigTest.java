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
        long day = 2 * 60 * 24; // 1 day

        assertEquals(0, Config.getBlockReward(0));
        assertEquals(0, Config.getBlockReward(day * 30));
        assertEquals(0, Config.getBlockReward(day * 90));
        assertEquals(5 * Unit.SEM, Config.getBlockReward(day * 180));
        assertEquals(5 * Unit.SEM, Config.getBlockReward(day * 360));
        assertEquals(5 * Unit.SEM, Config.getBlockReward(day * 720));
        assertEquals(0, Config.getBlockReward(day * 365 * 15));
    }

    @Test
    public void testNumberOfValidators() {
        long day = 2 * 60 * 24; // 1 day

        int last = 0;
        for (int i = 0; i < 60 * day; i++) {
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
