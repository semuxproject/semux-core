/**
 * Copyright (c) 2017-2018 The Semux Developers
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
    public HdKeyPair getRootAddressFromSeed(byte[] seed, KeyVersion keyVersion, CoinType coinType) {
        HdKeyPair masterAddress = hdKeyGenerator.getAddressFromSeed(seed, keyVersion, coinType);
        HdKeyPair purposeAddress = hdKeyGenerator.getAddress(masterAddress, PURPOSE, true);
        HdKeyPair coinTypeAddress = hdKeyGenerator.getAddress(purposeAddress, coinType.getCoinType(), true);
        HdKeyPair accountAddress = hdKeyGenerator.getAddress(coinTypeAddress, ACCOUNT, true);
        HdKeyPair changeAddress = hdKeyGenerator.getAddress(accountAddress, CHANGE, coinType.getAlwaysHardened());

        return changeAddress;
    }

    public HdKeyPair getAddress(HdKeyPair address, int index) {
        return hdKeyGenerator.getAddress(address, index, address.getCoinType().getAlwaysHardened());
    }
}
