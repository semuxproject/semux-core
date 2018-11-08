/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.util.Collections;
import java.util.Map;

import org.semux.Network;
import org.semux.core.Fork;

public class DevnetConfig extends AbstractConfig {

    public DevnetConfig(String dataDir) {
        super(dataDir, Network.DEVNET, Constants.DEVNET_VERSION);
        this.netMaxInboundConnectionsPerIp = Integer.MAX_VALUE;
    }

    @Override
    public Map<Long, byte[]> checkpoints() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Fork, Long> forkActivationCheckpoints() {
        return Collections.emptyMap();
    }
}
