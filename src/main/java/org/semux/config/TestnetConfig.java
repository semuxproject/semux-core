/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import static org.semux.net.Capability.SEM_TESTNET;

import java.util.Collections;
import java.util.Map;

import org.semux.Network;
import org.semux.net.CapabilitySet;

public class TestnetConfig extends AbstractConfig {

    public TestnetConfig(String dataDir) {
        super(dataDir, Network.TESTNET, Constants.TESTNET_VERSION);
        // testnet allows a much larger block size for performance tuning (10MB)
        maxBlockTransactionsSize = 10 * 1024 * 1024;
    }

    @Override
    public CapabilitySet capabilitySet() {
        return CapabilitySet.of(SEM_TESTNET);
    }

    @Override
    public Map<Long, Byte[]> checkpoints() {
        // we don't set checkpoints for the public testnet as the testnet can be reset
        // at anytime
        return Collections.emptyMap();
    }
}
