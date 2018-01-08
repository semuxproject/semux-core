/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DevNetConfigTest {

    @Test
    public void testNetworkId() {
        Config config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);
        assertEquals(Constants.DEV_NET_ID, config.networkId());

        System.out.println(DevNetConfig.KEY.toAddressString());
    }

}
