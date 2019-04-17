/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.core.bip32;

import java.io.UnsupportedEncodingException;

import org.semux.core.bip32.wallet.CoinType;
import org.semux.core.bip32.wallet.HdAddress;
import org.semux.core.bip32.wallet.HdKeyGenerator;

public abstract class BaseVectorTest {

    public HdAddress masterNode;
    public HdKeyGenerator hdKeyGenerator = new HdKeyGenerator();

    public BaseVectorTest() throws UnsupportedEncodingException {
        masterNode = hdKeyGenerator.getAddressFromSeed(getSeed(), Network.mainnet, CoinType.bitcoin);
    }

    protected abstract byte[] getSeed();
}
