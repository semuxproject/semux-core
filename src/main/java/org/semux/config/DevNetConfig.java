/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

public class DevNetConfig extends AbstractConfig {

    public DevNetConfig(String dataDir) {
        super(dataDir, Constants.DEVNET_ID, Constants.DEVNET_VERSION);
        this.netMaxInboundConnectionsPerIp = Integer.MAX_VALUE;
    }
}
