/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

public class TestNetConfig extends AbstractConfig {

    public TestNetConfig(String dataDir) {
        super(dataDir, Constants.TEST_NET_ID, (short) 0);
    }
}
