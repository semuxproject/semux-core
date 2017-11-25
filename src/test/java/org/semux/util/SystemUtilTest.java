/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtilTest {

    private Logger logger = LoggerFactory.getLogger(SystemUtilTest.class);

    @Test
    public void testCompareVersion() {
        assertEquals(0, SystemUtil.compareVersion("1.0.0", "1.0.0"));
        assertEquals(1, SystemUtil.compareVersion("1.0.0", "1.0.0-alpha"));
        assertEquals(1, SystemUtil.compareVersion("2.0.1", "1.0.2"));
        assertEquals(-1, SystemUtil.compareVersion("2.0.1-beta", "2.0.1-beta.1"));
    }

    @Test
    public void testGetIp() {
        String ip = SystemUtil.getIp();
        logger.info("IP address = {}", ip);
        assertNotNull(ip);
        assertFalse(ip.equals("127.0.0.1"));
    }

    @Test
    public void testBench() {
        logger.info("benchmark result = {}", SystemUtil.bench());
    }
}
