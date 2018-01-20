/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import org.semux.Network;

public class MainnetConfig extends AbstractConfig {

    public MainnetConfig(String dataDir) {
        super(dataDir, Network.MAINNET, Constants.MAINNET_VERSION);
    }
}