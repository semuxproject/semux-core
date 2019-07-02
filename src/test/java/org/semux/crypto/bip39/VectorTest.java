/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip39;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.semux.crypto.Hex;
import org.semux.crypto.bip32.Base58;
import org.semux.crypto.bip32.CoinType;
import org.semux.crypto.bip32.HdKeyGenerator;
import org.semux.crypto.bip32.HdKeyPair;
import org.semux.crypto.bip32.key.KeyVersion;
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
            Language lang = Language.valueOf(language.toUpperCase());
            for (Vector vector : vectors) {
                String password = vector.getPassphrase() == null ? defaultPassword : vector.getPassphrase();
                logger.info(lang + " " + vector.getMnemonic());
                String words = generator.getWordlist(vector.getEntropy(), lang);
                Assert.assertEquals(vector.getMnemonic(), words);
                byte[] seed = generator.getSeedFromWordlist(words, password, lang);
                Assert.assertEquals(vector.getSeed(), Hex.encode(seed));
                HdKeyPair address = keyGenerator.getMasterKeyPairFromSeed(seed, KeyVersion.MAINNET, CoinType.BITCOIN);
                Assert.assertEquals(vector.getHdKey(), Base58.encode(address.getPrivateKey().getKey()));
            }
        }
    }
}
