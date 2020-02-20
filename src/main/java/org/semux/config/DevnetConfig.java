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

import org.semux.Network;
import org.semux.core.Fork;

public class DevnetConfig extends AbstractConfig {

    public DevnetConfig(String dataDir) {
        super(dataDir, Network.DEVNET, Constants.DEVNET_VERSION);

        this.netMaxInboundConnectionsPerIp = Integer.MAX_VALUE;

        this.forkUniformDistributionEnabled = true;
        this.forkVirtualMachineEnabled = true;
        this.forkVotingPrecompiledUpgradeEnabled = true;

        // set fast blocks
        bftNewHeightTimeout = 1000L;
        bftProposeTimeout = 2000L;
        bftValidateTimeout = 1000L;
        bftPreCommitTimeout = 1000L;
        bftCommitTimeout = 1000L;
        bftFinalizeTimeout = 1000L;
    }

    @Override
    public Map<Long, byte[]> checkpoints() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Fork, Long> manuallyActivatedForks() {

        Map<Fork, Long> forks = new HashMap<>();
        forks.put(Fork.UNIFORM_DISTRIBUTION, 1l);
        forks.put(Fork.VIRTUAL_MACHINE, 1l);
        forks.put(Fork.VOTING_PRECOMPILED_UPGRADE, 1l);

        return forks;
    }
}
