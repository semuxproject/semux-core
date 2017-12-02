/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.util.List;

import org.semux.core.Unit;
import org.semux.crypto.Hash;
import org.semux.util.Bytes;
import org.semux.util.SystemUtil;

public class MainNetConfig extends AbstractConfig {

    public MainNetConfig(String dataDir) {
        super(dataDir);
    }

    @Override
    public long getBlockReward(long number) {
        if (number <= 75_000_000L) {
            return 1 * Unit.SEM;
        } else {
            return 0;
        }
    }

    @Override
    public long getValidatorUpdateInterval() {
        return 64L * 2L;
    }

    @Override
    public int getNumberOfValidators(long number) {
        long step = 2 * 60 * 2;

        if (number < 48 * step) {
            return (int) (16 + number / step);
        } else {
            return 64;
        }
    }

    @Override
    public String getPrimaryValidator(List<String> validators, long height, int view) {
        byte[] key = Bytes.merge(Bytes.of(height), Bytes.of(view));
        return validators.get((Hash.h256(key)[0] & 0xff) % validators.size());
    }

    @Override
    public String getClientId() {
        return String.format("%s/v%s/%s/%s", Constants.CLIENT_NAME, Constants.CLIENT_VERSION,
                SystemUtil.getOS().toString(), SystemUtil.getOSArch());
    }
}
