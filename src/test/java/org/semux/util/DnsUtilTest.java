/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

public class DnsUtilTest {

    @Test
    public void testQueryTxt() {
        String hostname = "google.com";
        List<String> txts = DnsUtil.queryTxt(hostname);
        for (String txt : txts) {
            if (txt.contains("spf")) {
                return;
            }
        }
        fail("Failed to get the TXT records of " + hostname);
    }
}
