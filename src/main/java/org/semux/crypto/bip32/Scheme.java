/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.crypto.bip32;

public enum Scheme {
    /**
     * Reference: https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
     */
    BIP32("Bitcoin seed"),

    /**
     * Reference: https://github.com/satoshilabs/slips/blob/master/slip-0010.md
     */
    SLIP10_ED25519("ed25519 seed"),

    /**
     * Reference: https://cardanolaunch.com/assets/Ed25519_BIP.pdf
     *
     * Implementation:
     * https://github.com/LedgerHQ/orakolo/blob/master/src/python/orakolo/HDEd25519.py
     */
    BIP32_ED25519("ed25519 seed");

    private final String seed;

    Scheme(String seed) {
        this.seed = seed;
    }

    public String getSeed() {
        return seed;
    }
}
