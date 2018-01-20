/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;

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
        Instant begin = Instant.now();
        String ip = SystemUtil.getIp();
        logger.info("IP address = {}, took {} ms", ip, Duration.between(begin, Instant.now()).toMillis());

        assertFalse("127.0.0.1".equals(ip));
    }

    @Test
    public void testGetAvailableMemorySize() {
        long size = SystemUtil.getAvailableMemorySize();
        logger.info("Available memory size = {} MB", size / 1024L / 1024L);

        assertTrue(size > 0);
        assertTrue(size < 64L * 1024L * 1024L * 1024L);
        assertTrue(size != 0xffffffffL);
    }

    @Test
    public void testGetUsedHeapSize() {
        long size = SystemUtil.getUsedHeapSize();
        logger.info("Used heap size = {} MB", size / 1024L / 1024L);

        assertTrue(size > 0);
        assertTrue(size < 4L * 1024L * 1024L * 1024L);
    }

    @Test
    public void testBench() {
        logger.info("System benchmark result = {}", SystemUtil.bench());
    }
}
