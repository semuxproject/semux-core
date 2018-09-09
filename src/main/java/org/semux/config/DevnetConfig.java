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
import org.semux.consensus.ValidatorActivatedFork;

public class DevnetConfig extends AbstractConfig {

    public DevnetConfig(String dataDir) {
        super(dataDir, Network.DEVNET, Constants.DEVNET_VERSION);
        this.netMaxInboundConnectionsPerIp = Integer.MAX_VALUE;
    }

    private static final Map<ValidatorActivatedFork, Long> forkActivationCheckpoints;
    static {
        HashMap<ValidatorActivatedFork, Long> initForkActivationCheckpoints = new HashMap<>();

        // all forks should be activated upon startup.
        initForkActivationCheckpoints.put(ValidatorActivatedFork.UNIFORM_DISTRIBUTION, 1L);

        forkActivationCheckpoints = MapUtils.unmodifiableMap(initForkActivationCheckpoints);
    }

    @Override
    public Map<Long, byte[]> checkpoints() {
        return Collections.emptyMap();
    }

    @Override
    public Map<ValidatorActivatedFork, Long> forkActivationCheckpoints() {
        return forkActivationCheckpoints;
    }
}
