/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip32;

public enum CoinType {
    BITCOIN(Scheme.BIP32, 0, false),

    SEMUX(Scheme.SLIP10_ED25519, 7562605, true);

    private final Scheme scheme;
    private final long coinType;
    private boolean alwaysHardened;

    CoinType(Scheme scheme, long coinType, boolean alwaysHardened) {

        this.scheme = scheme;
        this.coinType = coinType;
        this.alwaysHardened = alwaysHardened;
    }

    /**
     * Get the curve
     *
     * @return curve
     */
    public Scheme getScheme() {
        return scheme;
    }

    /**
     * get the coin type
     *
     * @return coin type
     */
    public long getCoinType() {
        return coinType;
    }

    /**
     * get whether the addresses must always be hardened
     *
     * @return always hardened
     */
    public boolean getAlwaysHardened() {
        return alwaysHardened;
    }
}
