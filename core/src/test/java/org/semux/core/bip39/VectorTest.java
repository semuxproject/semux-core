/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.bip39;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.semux.core.bip32.Base58;
import org.semux.core.bip32.Network;
import org.semux.core.bip32.extern.Hex;
import org.semux.core.bip32.wallet.CoinType;
import org.semux.core.bip32.wallet.HdAddress;
import org.semux.core.bip32.wallet.HdKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorTest {

    private static final Logger logger = LoggerFactory.getLogger(VectorTest.class);

    @Test
    public void testVectors() throws IOException {
        MnemonicGenerator generator = new MnemonicGenerator();

        VectorReader reader = new VectorReader();
        Map<String, List<Vector>> vectorSets = reader.getVectors();
        HdKeyGenerator keyGenerator = new HdKeyGenerator();

        String defaultPassword = "TREZOR";
        for (String language : vectorSets.keySet()) {
            List<Vector> vectors = vectorSets.get(language);
            Language lang = Language.valueOf(language);
            for (Vector vector : vectors) {
                String password = vector.getPassphrase() == null ? defaultPassword : vector.getPassphrase();
                logger.info(lang + " " + vector.getMnemonic());
                String words = generator.getWordlist(vector.getEntropy(), lang);
                Assert.assertEquals(vector.getMnemonic(), words);
                byte[] seed = generator.getSeedFromWordlist(words, password, lang);
                Assert.assertEquals(vector.getSeed(), Hex.encode(seed));
                HdAddress address = keyGenerator.getAddressFromSeed(seed, Network.mainnet, CoinType.bitcoin);
                Assert.assertEquals(vector.getHdKey(), Base58.encode(address.getPrivateKey().getKey()));
            }
        }
    }
}
