/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.util.Collections;
import java.util.Map;

import org.semux.Network;
import org.semux.core.Fork;

/**
 * The unit tests were written with a specific configuration in mind, however
 * the devnet config is changeable for users needs and to aid in local
 * development
 *
 * So we introduce a new network configuration with no forks enabled and normal
 * block times.
 *
 */
public class UnitTestnetConfig extends AbstractConfig {

    public UnitTestnetConfig(String dataDir) {
        super(dataDir, Network.DEVNET, Constants.DEVNET_VERSION);

        this.netMaxInboundConnectionsPerIp = Integer.MAX_VALUE;

        this.forkUniformDistributionEnabled = true;
        this.forkVirtualMachineEnabled = true;
        this.forkVotingPrecompiledUpgradeEnabled = true;
    }

    @Override
    public Map<Long, byte[]> checkpoints() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Fork, Long> manuallyActivatedForks() {
        return Collections.emptyMap();
    }
}
