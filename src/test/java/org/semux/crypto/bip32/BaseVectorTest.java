/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip32;

import java.io.UnsupportedEncodingException;

import org.semux.crypto.bip32.key.KeyVersion;

public abstract class BaseVectorTest {

    public HdKeyPair masterNode;
    public HdKeyGenerator hdKeyGenerator = new HdKeyGenerator();

    public BaseVectorTest() throws UnsupportedEncodingException {
        masterNode = hdKeyGenerator.getMasterKeyPairFromSeed(getSeed(), KeyVersion.MAINNET, CoinType.BITCOIN);
    }

    protected abstract byte[] getSeed();
}
