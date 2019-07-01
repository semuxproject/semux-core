/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.crypto.bip32;

import org.junit.Assert;
import org.junit.Test;
import org.semux.crypto.Hex;
import org.semux.crypto.bip32.key.HdPublicKey;
import org.semux.crypto.bip32.key.KeyVersion;

public class PublicKeyChainTest {

    public static final byte[] SEED = Hex.decode("000102030405060708090a0b0c0d0e0f");

    HdKeyGenerator generator = new HdKeyGenerator();

    @Test
    public void testPubKey0() {
        HdKeyPair rootKeyPair = generator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.BITCOIN);
        HdKeyPair childKeyPair = generator.getChildKeyPair(rootKeyPair, 0, false);
        // test that the pub key chain generated from only public key matches the other
        HdPublicKey pubKey = generator.getChildPublicKey(rootKeyPair.getPublicKey(), 0, false, Scheme.BIP32);
        Assert.assertArrayEquals(childKeyPair.getPublicKey().getKey(), pubKey.getKey());
    }
}
