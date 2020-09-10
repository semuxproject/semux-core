/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip44;

import org.semux.crypto.bip32.CoinType;
import org.semux.crypto.bip32.HdKeyGenerator;
import org.semux.crypto.bip32.HdKeyPair;
import org.semux.crypto.bip32.key.KeyVersion;

/**
 * Utility class for BIP-44 paths
 */
public class Bip44 {
    private HdKeyGenerator hdKeyGenerator = new HdKeyGenerator();
    // purpose is hardcoded to 44'
    private static final int PURPOSE = 44;
    // we support just one user account
    private static final long ACCOUNT = 0;
    // we support just external addresses, 0 is 'external'
    private static final int CHANGE = 0;

    /**
     * Get a root account address for a given seed
     *
     * @param seed
     *            seed
     * @param keyVersion
     *            network
     * @param coinType
     *            coinType
     * @return
     */
    public HdKeyPair getRootKeyPairFromSeed(byte[] seed, KeyVersion keyVersion, CoinType coinType) {
        HdKeyPair masterKey = hdKeyGenerator.getMasterKeyPairFromSeed(seed, keyVersion, coinType);
        HdKeyPair purposeKey = hdKeyGenerator.getChildKeyPair(masterKey, PURPOSE, true);
        HdKeyPair coinTypeKey = hdKeyGenerator.getChildKeyPair(purposeKey, coinType.getCoinType(), true);
        HdKeyPair accountKey = hdKeyGenerator.getChildKeyPair(coinTypeKey, ACCOUNT, true);
        HdKeyPair changeKey = hdKeyGenerator.getChildKeyPair(accountKey, CHANGE, coinType.getAlwaysHardened());

        return changeKey;
    }

    public HdKeyPair getChildKeyPair(HdKeyPair parent, int index) {
        return hdKeyGenerator.getChildKeyPair(parent, index, parent.getCoinType().getAlwaysHardened());
    }
}
