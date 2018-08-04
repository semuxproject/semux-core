/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.semux.Network;
import org.semux.consensus.ValidatorActivatedFork;
import org.semux.crypto.Hex;

public class MainnetConfig extends AbstractConfig {

    private static final Map<Long, byte[]> checkpoints;
    static {
        HashMap<Long, byte[]> initCheckpoints = new HashMap<>();

        // The first day of semux mainnet:
        initCheckpoints.put(2880L, Hex.decode0x("0xc494425a534d035d9ceb1c05d84ff23b39e9940c5cbd1cbab35fafaffa711e3c"));

        // One regular checkpoint for every 60 days:
        initCheckpoints.put(172800L,
                Hex.decode0x("0xe0f8694ab43cddc1cf99fdee887297522111df0b82a07816350e0ffb801d0253"));

        // Forks:
        // UNIFORM_DISTRIBUTION fork
        initCheckpoints.put(233514L,
                Hex.decode0x("0x5afad5833a7f29299fc209e0914bdcd6824975897a8a4dcfb05448e30bef3d2a"));

        checkpoints = MapUtils.unmodifiableMap(initCheckpoints);
    }

    private static final Map<ValidatorActivatedFork, Long> forkActivationCheckpoints;
    static {
        HashMap<ValidatorActivatedFork, Long> initForkActivationCheckpoints = new HashMap<>();

        initForkActivationCheckpoints.put(ValidatorActivatedFork.UNIFORM_DISTRIBUTION, 233514L);

        forkActivationCheckpoints = MapUtils.unmodifiableMap(initForkActivationCheckpoints);
    }

    public MainnetConfig(String dataDir) {
        super(dataDir, Network.MAINNET, Constants.MAINNET_VERSION);
    }

    @Override
    public Map<Long, byte[]> checkpoints() {
        return checkpoints;
    }

    @Override
    public Map<ValidatorActivatedFork, Long> forkActivationCheckpoints() {
        return forkActivationCheckpoints;
    }
}