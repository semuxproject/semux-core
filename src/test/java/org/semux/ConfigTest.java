/**
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
        long total = 0;
        for (int i = 1; i <= 100_000_000; i++) {
            total += Config.getBlockReward(i);
        }
        assertEquals(75_000_000 * Unit.SEM, total);
    }

    @Test
    public void testNumberOfValidators() {
        int last = 0;
        for (int i = 0; i < 60 * Config.BLOCKS_PER_DAY; i++) {
            int n = Config.getNumberOfValidators(i);
            if (n != last) {
                assertTrue(n > last && (n - last == 1 || last == 0));
                logger.info("block = {}, validators = {}", i, n);
                last = n;
            }
        }

        assertEquals(64, last);
    }
}
