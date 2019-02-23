/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.core.bip32;

import org.semux.core.bip32.Network;
import org.semux.core.bip32.extern.Hex;
import org.semux.core.bip32.wallet.CoinType;
import org.semux.core.bip32.wallet.HdAddress;
import org.semux.core.bip32.wallet.HdKeyGenerator;
import org.semux.core.bip32.wallet.key.Curve;
import org.semux.core.bip32.wallet.key.HdPublicKey;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class PublicKeyChainTest {

    public static final byte[] SEED = Hex.decode("000102030405060708090a0b0c0d0e0f");

    HdKeyGenerator generator = new HdKeyGenerator();

    @Test
    public void testPubKey0() throws UnsupportedEncodingException {
        HdAddress rootAddress = generator.getAddressFromSeed(SEED, Network.mainnet, CoinType.bitcoin);
        HdAddress address = generator.getAddress(rootAddress, 0, false);
        // test that the pub key chain generated from only public key matches the other
        HdPublicKey pubKey = generator.getPublicKey(rootAddress.getPublicKey(), 0, false, Curve.bitcoin);
        Assert.assertArrayEquals(address.getPublicKey().getKey(), pubKey.getKey());
    }
}
