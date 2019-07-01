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
import org.semux.crypto.bip32.key.KeyVersion;

public class Slip10Test {

    public static final byte[] SEED = Hex.decode("000102030405060708090a0b0c0d0e0f");
    public HdKeyGenerator hdKeyGenerator = new HdKeyGenerator();

    @Test
    public void testVectorOneMaster() {
        byte[] fingerprint = Hex.decode("00000000");
        byte[] chainCode = Hex.decode("90046a93de5380a72b5e45010748567d5ea02bbf6522f979e05c0d8d8ca9fffb");
        byte[] privateKey = Hex.decode("2b4be7f19ee27bbf30c667b642d5f4aa69fd169872f8fc3059c08ebae2eb19e7");
        byte[] publicKey = Hex.decode("00a4b2856bfec510abab89753fac1ac0e1112364e7d250545963f135f2a33188ed");

        HdKeyPair master = hdKeyGenerator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.SEMUX_SLIP10);

        Assert.assertArrayEquals(fingerprint, master.getPrivateKey().getFingerprint());
        Assert.assertArrayEquals(chainCode, master.getPrivateKey().getChainCode());
        Assert.assertArrayEquals(privateKey, master.getPrivateKey().getKeyData());
        Assert.assertArrayEquals(publicKey, master.getPublicKey().getKeyData());
        Assert.assertEquals(Scheme.SLIP10_ED25519, master.getCoinType().getScheme());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnableToPublicChain() {
        HdKeyPair master = hdKeyGenerator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.SEMUX_SLIP10);
        hdKeyGenerator.getChildPublicKey(master.getPublicKey(), 0, false, Scheme.SLIP10_ED25519);
    }

    @Test
    public void testVector0H() {
        byte[] fingerprint = Hex.decode("ddebc675");
        byte[] chainCode = Hex.decode("8b59aa11380b624e81507a27fedda59fea6d0b779a778918a2fd3590e16e9c69");
        byte[] privateKey = Hex.decode("68e0fe46dfb67e368c75379acec591dad19df3cde26e63b93a8e704f1dade7a3");
        byte[] publicKey = Hex.decode("008c8a13df77a28f3445213a0f432fde644acaa215fc72dcdf300d5efaa85d350c");

        HdKeyPair master = hdKeyGenerator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.SEMUX_SLIP10);
        HdKeyPair child = hdKeyGenerator.getChildKeyPair(master, 0, true);

        Assert.assertArrayEquals(fingerprint, child.getPrivateKey().getFingerprint());
        Assert.assertArrayEquals(chainCode, child.getPrivateKey().getChainCode());
        Assert.assertArrayEquals(privateKey, child.getPrivateKey().getKeyData());
        Assert.assertArrayEquals(publicKey, child.getPublicKey().getKeyData());
        Assert.assertEquals(Scheme.SLIP10_ED25519, child.getCoinType().getScheme());
    }

    @Test
    public void testVector0H1H() {
        byte[] fingerprint = Hex.decode("13dab143");
        byte[] chainCode = Hex.decode("a320425f77d1b5c2505a6b1b27382b37368ee640e3557c315416801243552f14");
        byte[] privateKey = Hex.decode("b1d0bad404bf35da785a64ca1ac54b2617211d2777696fbffaf208f746ae84f2");
        byte[] publicKey = Hex.decode("001932a5270f335bed617d5b935c80aedb1a35bd9fc1e31acafd5372c30f5c1187");

        HdKeyPair master = hdKeyGenerator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.SEMUX_SLIP10);
        HdKeyPair child = hdKeyGenerator.getChildKeyPair(master, 0, true);
        child = hdKeyGenerator.getChildKeyPair(child, 1, true);

        Assert.assertArrayEquals(fingerprint, child.getPrivateKey().getFingerprint());
        Assert.assertArrayEquals(chainCode, child.getPrivateKey().getChainCode());
        Assert.assertArrayEquals(privateKey, child.getPrivateKey().getKeyData());
        Assert.assertArrayEquals(publicKey, child.getPublicKey().getKeyData());
        Assert.assertEquals(Scheme.SLIP10_ED25519, child.getCoinType().getScheme());
    }

    @Test
    public void testVector0H1H2H() {
        byte[] fingerprint = Hex.decode("ebe4cb29");
        byte[] chainCode = Hex.decode("2e69929e00b5ab250f49c3fb1c12f252de4fed2c1db88387094a0f8c4c9ccd6c");
        byte[] privateKey = Hex.decode("92a5b23c0b8a99e37d07df3fb9966917f5d06e02ddbd909c7e184371463e9fc9");
        byte[] publicKey = Hex.decode("00ae98736566d30ed0e9d2f4486a64bc95740d89c7db33f52121f8ea8f76ff0fc1");

        HdKeyPair master = hdKeyGenerator.getMasterKeyPairFromSeed(SEED, KeyVersion.MAINNET, CoinType.SEMUX_SLIP10);
        HdKeyPair child = hdKeyGenerator.getChildKeyPair(master, 0, true);
        child = hdKeyGenerator.getChildKeyPair(child, 1, true);
        child = hdKeyGenerator.getChildKeyPair(child, 2, true);

        Assert.assertArrayEquals(fingerprint, child.getPrivateKey().getFingerprint());
        Assert.assertArrayEquals(chainCode, child.getPrivateKey().getChainCode());
        Assert.assertArrayEquals(privateKey, child.getPrivateKey().getKeyData());
        Assert.assertArrayEquals(publicKey, child.getPublicKey().getKeyData());
        Assert.assertEquals(Scheme.SLIP10_ED25519, child.getCoinType().getScheme());
    }
}
