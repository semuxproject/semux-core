/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.security.spec.InvalidKeySpecException;

import org.semux.crypto.CryptoException;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;

public class DevNetConfig extends AbstractConfig {

    public static final String PRIVATE_KEY = "0x302e020100300506032b657004220420acbd5f2cb2b6053f704376d12df99f2aa163d267a755c7f1d9fe55d2a2dc5405";

    public static EdDSA KEY;
    static {
        try {
            KEY = new EdDSA(Hex.decode0x(PRIVATE_KEY));
        } catch (InvalidKeySpecException | CryptoException e) {
            throw new RuntimeException(e);
        }
    }

    public DevNetConfig(String dataDir) {
        super(dataDir, Constants.DEV_NET_ID, Constants.DEV_NET_VERSION);
        this.netMaxInboundConnectionsPerIp = Integer.MAX_VALUE;
    }
}
