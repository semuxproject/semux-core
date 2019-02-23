/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.bip32;

import org.semux.core.bip32.extern.Hex;

/**
 * Defined network values for key generation
 */
public enum Network {
    mainnet("0x0488ADE4", "0x0488B21E"), testnet("0x04358394", "0x043587CF");

    private final byte[] privatePrefix;
    private final byte[] publicPrefix;

    Network(String privatePrefix, String publicPrefix) {
        this.privatePrefix = Hex.decode0x(privatePrefix);
        this.publicPrefix = Hex.decode0x(publicPrefix);
    }

    public byte[] getPrivateKeyVersion() {
        return privatePrefix;
    }

    public byte[] getPublicKeyVersion() {
        return publicPrefix;
    }
}
