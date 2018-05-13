/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import static org.semux.net.Capability.SEM;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.semux.Network;
import org.semux.crypto.Hex;
import org.semux.net.CapabilitySet;

public class MainnetConfig extends AbstractConfig {

    public MainnetConfig(String dataDir) {
        super(dataDir, Network.MAINNET, Constants.MAINNET_VERSION);
    }

    @Override
    public CapabilitySet capabilitySet() {
        return CapabilitySet.of(SEM);
    }

    @Override
    public Map<Long, Byte[]> checkpoints() {
        HashMap<Long, Byte[]> checkpoints = new HashMap<>();

        // the first day of semux mainnet
        checkpoints.put(2880L, ArrayUtils
                .toObject(Hex.decode0x("0xc494425a534d035d9ceb1c05d84ff23b39e9940c5cbd1cbab35fafaffa711e3c")));

        // one checkpoint for every 60 days
        checkpoints.put(172800L, ArrayUtils
                .toObject(Hex.decode0x("0xe0f8694ab43cddc1cf99fdee887297522111df0b82a07816350e0ffb801d0253")));

        return MapUtils.unmodifiableMap(checkpoints);
    }
}