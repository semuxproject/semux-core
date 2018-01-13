/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mitlicense.php
 */
package org.semux.config;

public class MainnetConfig extends AbstractConfig {

    public MainnetConfig(String dataDir) {
        super(dataDir, Constants.MAINNET_ID, Constants.MAINNET_VERSION);
    }
}