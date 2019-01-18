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

public class TestnetConfig extends AbstractConfig {

    public TestnetConfig(String dataDir) {
        super(dataDir, Network.TESTNET, Constants.TESTNET_VERSION);
        // testnet allows a much larger block size for performance tuning (10MB)
        maxBlockTransactionsSize = 10 * 1024 * 1024;
    }

    @Override
    public Map<Long, byte[]> checkpoints() {
        // we don't set checkpoints for the public testnet as the testnet can be reset
        // at anytime
        return Collections.emptyMap();
    }

    @Override
    public Map<Fork, Long> forkActivationCheckpoints() {
        return Collections.emptyMap();
    }

    /**
     * Testnet maxes out at 15 validators to stop dead validators from breaking
     * concensus
     * 
     * @param number
     * @return
     */
    @Override
    public int getNumberOfValidators(long number) {

        // lazy fork to adjust number down to unstick testnet to more reasonable max
        // number
        if (number <= 751638) {
            return super.getNumberOfValidators(number);
        } else {
            return 15;
        }
    }
}
