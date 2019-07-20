/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip32;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.crypto.Hex;
import org.semux.crypto.bip32.key.KeyVersion;
import org.semux.crypto.bip39.Language;
import org.semux.crypto.bip39.MnemonicGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip32Ed25519Test {

    private static final Logger logger = LoggerFactory.getLogger(Bip32Ed25519Test.class);

    private byte[] SEED = new MnemonicGenerator().getSeedFromWordlist(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            "",
            Language.ENGLISH);

    private HdKeyGenerator generator = new HdKeyGenerator();
    private HdKeyPair root = generator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.SEMUX_BIP32_ED25519);

    @Test
    public void testRoot() {
        String seed = "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4";
        String kL = "402b03cd9c8bed9ba9f9bd6cd9c315ce9fcc59c7c25d37c85a36096617e69d41";
        String kR = "8e35cb4a3b737afd007f0688618f21a8831643c0e6c77fc33c06026d2a0fc938";
        String A = "291ea7aa3766cd26a3a8688375aa07b3fed73c13d42543a9f19a48dc8b6bfd07";
        String c = "32596435e70647d7d98ef102a32ea40319ca8fb6c851d7346d3bd8f9d1492658";

        logger.info("k = " + Hex.encode(root.getPrivateKey().getKeyData()));
        logger.info("A = " + Hex.encode(root.getPublicKey().getKeyData()));
        logger.info("c = " + Hex.encode(root.getPublicKey().getChainCode()));

        assertEquals(seed, Hex.encode(SEED));
        assertEquals(kL + kR, Hex.encode(root.getPrivateKey().getKeyData()));
        assertEquals(A, Hex.encode(root.getPublicKey().getKeyData()));
        assertEquals(c, Hex.encode(root.getPublicKey().getChainCode()));
    }

    @Test
    public void testOne() {
        // path = "42'/1/2";
        String kL = "b02160bb753c495687eb0b0e0628bf637e85fd3aadac109847afa2ad20e69d41";
        String kR = "00ea111776aabeb85446b186110f8337a758681c96d5d01d5f42d34baf97087b";
        String A = "bc738b13faa157ce8f1534ddd9299e458be459f734a5fa17d1f0e73f559a69ee";
        String c = "c52916b7bb856bd1733390301cdc22fd2b0d5e6fab9908d55fd1bed13bccbb36";

        HdKeyPair child1 = generator.getChildKeyPair(root, 42, true);
        HdKeyPair child2 = generator.getChildKeyPair(child1, 1, false);
        HdKeyPair child3 = generator.getChildKeyPair(child2, 2, false);

        logger.info("k = " + Hex.encode(child3.getPrivateKey().getKeyData()));
        logger.info("A = " + Hex.encode(child3.getPublicKey().getKeyData()));
        logger.info("c = " + Hex.encode(child3.getPublicKey().getChainCode()));

        assertEquals(kL + kR, Hex.encode(child3.getPrivateKey().getKeyData()));
        assertEquals(A, Hex.encode(child3.getPublicKey().getKeyData()));
        assertEquals(c, Hex.encode(child3.getPublicKey().getChainCode()));
    }

    @Test
    public void testTwo() {
        // path = "42'/3'/5";
        String kL = "78164270a17f697b57f172a7ac58cfbb95e007fdcd968c8c6a2468841fe69d41";
        String kR = "15c846a5d003f7017374d12105c25930a2bf8c386b7be3c470d8226f3cad8b6b";
        String A = "286b8d4ef3321e78ecd8e2585e45cb3a8c97d3f11f829860ce461df992a7f51c";
        String c = "7e64c416800883256828efc63567d8842eda422c413f5ff191512dfce7790984";

        HdKeyPair child1 = generator.getChildKeyPair(root, 42, true);
        HdKeyPair child2 = generator.getChildKeyPair(child1, 3, true);
        HdKeyPair child3 = generator.getChildKeyPair(child2, 5, false);

        logger.info("k = " + Hex.encode(child3.getPrivateKey().getKeyData()));
        logger.info("A = " + Hex.encode(child3.getPublicKey().getKeyData()));
        logger.info("c = " + Hex.encode(child3.getPublicKey().getChainCode()));

        assertEquals(kL + kR, Hex.encode(child3.getPrivateKey().getKeyData()));
        assertEquals(A, Hex.encode(child3.getPublicKey().getKeyData()));
        assertEquals(c, Hex.encode(child3.getPublicKey().getChainCode()));
    }
}
