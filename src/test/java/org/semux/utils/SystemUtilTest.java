package org.semux.utils;

import static org.junit.Assert.assertEquals;
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
    }
}
