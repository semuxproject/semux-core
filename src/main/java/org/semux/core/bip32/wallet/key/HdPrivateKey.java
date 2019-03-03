/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.core.bip32.wallet.key;

import org.semux.core.bip32.wallet.key.HdKey;

/**
 * Defines a key with a given private key
 */
public class HdPrivateKey extends HdKey {
    private byte[] privateKey;

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }
}
