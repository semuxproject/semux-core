/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip32.key;

import org.semux.crypto.Hex;

/**
 * Defined network values for key generation
 */
public enum KeyVersion {
    MAINNET("0x0488ADE4", "0x0488B21E"),

    TESTNET("0x04358394", "0x043587CF");

    private final byte[] privatePrefix;
    private final byte[] publicPrefix;

    KeyVersion(String privatePrefix, String publicPrefix) {
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
