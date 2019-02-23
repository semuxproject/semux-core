/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.bip39;

import org.semux.core.bip32.Base58;
import org.semux.core.bip32.Network;
import org.semux.core.bip32.extern.Hex;
import org.semux.core.bip32.wallet.CoinType;
import org.semux.core.bip32.wallet.HdAddress;
import org.semux.core.bip32.wallet.HdKeyGenerator;
import org.semux.core.bip39.Language;
import org.semux.core.bip39.MnemonicGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class VectorTest {

    @Test
    public void testVectors() throws IOException {
        MnemonicGenerator generator = new MnemonicGenerator();

        VectorReader reader = new VectorReader();
        List<Vector> vectors = reader.getVectors();
        HdKeyGenerator keyGenerator = new HdKeyGenerator();

        String password = "TREZOR";
        for (Vector vector : vectors) {
            String words = generator.getWordlist(vector.getEntropy(), Language.english);
            Assert.assertEquals(vector.getMnemonic(), words);
            byte[] seed = generator.getSeedFromWordlist(words, password, Language.english);
            Assert.assertEquals(vector.getSeed(), Hex.encode(seed));
            HdAddress address = keyGenerator.getAddressFromSeed(seed, Network.mainnet, CoinType.bitcoin);
            Assert.assertEquals(vector.getHdKey(), Base58.encode(address.getPrivateKey().getKey()));
        }
    }
}
