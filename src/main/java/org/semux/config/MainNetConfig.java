/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

public class MainNetConfig extends AbstractConfig {

    public MainNetConfig(String dataDir) {
        super(dataDir, Constants.MAIN_NET_ID, (short) 4);
    }
}
