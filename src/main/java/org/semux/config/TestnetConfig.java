/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import org.semux.Network;

public class TestnetConfig extends AbstractConfig {

    public TestnetConfig(String dataDir) {
        super(dataDir, Network.TESTNET, Constants.TESTNET_VERSION);
    }
}
