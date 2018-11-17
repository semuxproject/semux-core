/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.semux.Network;
import org.semux.core.Fork;

public class DevnetConfig extends AbstractConfig {

    public DevnetConfig(String dataDir) {
        super(dataDir, Network.DEVNET, Constants.DEVNET_VERSION);
        this.netMaxInboundConnectionsPerIp = Integer.MAX_VALUE;
    }

    private static final Map<Fork, Long> forkActivationCheckpoints;
    static {
        HashMap<Fork, Long> initForkActivationCheckpoints = new HashMap<>();

        // uncomment for local development in these forks
        // initForkActivationCheckpoints.put(Fork.UNIFORM_DISTRIBUTION, 1L);
        // initForkActivationCheckpoints.put(Fork.VIRTUAL_MACHINE, 1L);

        forkActivationCheckpoints = MapUtils.unmodifiableMap(initForkActivationCheckpoints);
    }

    @Override
    public Map<Long, byte[]> checkpoints() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Fork, Long> forkActivationCheckpoints() {
        return forkActivationCheckpoints;
    }
}
