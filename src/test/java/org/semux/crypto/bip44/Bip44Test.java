/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.crypto.bip44;

import org.junit.Test;
import org.semux.crypto.Hex;
import org.semux.crypto.bip32.CoinType;
import org.semux.crypto.bip32.HdKeyPair;
import org.semux.crypto.bip32.key.KeyVersion;

public class Bip44Test {
    private Bip44 bip44 = new Bip44();
    private byte[] seed = Hex.decode("abcdef");

    @Test
    public void testBitcoin() {
        HdKeyPair key = bip44.getRootKeyPairFromSeed(seed, KeyVersion.MAINNET, CoinType.BITCOIN);
        bip44.getChildKeyPair(key, 0);
    }

    @Test
    public void testSemux() {
        HdKeyPair key = bip44.getRootKeyPairFromSeed(seed, KeyVersion.MAINNET, CoinType.SEMUX);
        bip44.getChildKeyPair(key, 0);
    }
}
