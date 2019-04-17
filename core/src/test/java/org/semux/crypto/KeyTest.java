/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.crypto.Key.Signature;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.KeyPairGenerator;

public class KeyTest {

    private static final Logger logger = LoggerFactory.getLogger(KeyTest.class);

    @Test
    public void testKeyStorage() {
        Key key = new Key();

        System.out.println("pk_encoded     : " + Hex.encode(key.pk.getEncoded()));
        System.out.println("pk_algo        : " + key.pk.getAlgorithm());
        System.out.println("pk_format      : " + key.pk.getFormat());
        System.out.println("pk_A           : " + Hex.encode(key.pk.getAbyte()));
        System.out.println();
        System.out.println("sk_encoded     : " + Hex.encode(key.sk.getEncoded()));
        System.out.println("sk_algo        : " + key.sk.getAlgorithm());
        System.out.println("sk_format      : " + key.sk.getFormat());
        System.out.println("sk_seed        : " + Hex.encode(key.sk.getSeed()));
        System.out.println("sk_hash_of_seed: " + Hex.encode(key.sk.getH()));
    }

    @Test
    public void testMalleability() {
        Key key = new Key();
        byte[] data = Bytes.of("test");
        byte[] hash = Hash.h256(data);

        Signature sig = key.sign(hash);
        assertTrue(Key.verify(hash, sig));

        byte[] R = Arrays.copyOf(sig.getS(), 32);
        byte[] S = Arrays.copyOfRange(sig.getS(), 32, 64);

        BigInteger s = new BigInteger(1, S);
        BigInteger l = BigInteger.valueOf(2).pow(252).add(new BigInteger("27742317777372353535851937790883648493"));
        byte[] sPlusL = s.add(l).toByteArray();
        sPlusL = Arrays.copyOfRange(sPlusL, sPlusL.length - 32, sPlusL.length);

        Signature sig2 = new Signature(Bytes.merge(R, sPlusL), sig.getA());
        assertFalse(Key.verify(hash, sig2));
    }

    @Test
    public void testGenerateKeyPair() throws InvalidKeySpecException {
        Key key1 = new Key();

        assertEquals(Key.PUBLIC_KEY_LEN, key1.getPublicKey().length);
        assertEquals(Key.PRIVATE_KEY_LEN, key1.getPrivateKey().length);

        Key key2 = new Key(key1.getPrivateKey(), key1.getPublicKey());

        Assert.assertArrayEquals(key1.getPublicKey(), key2.getPublicKey());
        Assert.assertArrayEquals(key1.getPrivateKey(), key2.getPrivateKey());
    }

    @Test(expected = InvalidKeySpecException.class)
    public void testPublicPrivateKeyMismatch() throws InvalidKeySpecException {
        Key key1 = new Key();

        new Key(key1.getPrivateKey(), new byte[Key.PUBLIC_KEY_LEN]);
    }

    @Test
    public void testSignAndVerify() throws SignatureException {
        Key key = new Key();
        byte[] data = Bytes.of("test");

        byte[] hash = Hash.h256(data);
        byte[] sig = key.sign(hash).toBytes();

        assertEquals(Signature.LENGTH, sig.length);
        assertTrue(Key.verify(hash, sig));
        assertArrayEquals(key.getPublicKey(), Signature.fromBytes(sig).getPublicKey());
        assertArrayEquals(key.toAddress(), Signature.fromBytes(sig).getAddress());
    }

    @Test
    public void testSignLargeData() throws SignatureException {
        byte[] data = Bytes.random(1024 * 1024);

        Key key = new Key();
        Signature sig = key.sign(data);

        assertTrue(Key.verify(data, sig));
        assertArrayEquals(key.getPublicKey(), sig.getPublicKey());
    }

    @Test
    public void testInvalidSignature() throws SignatureException {
        byte[] data = Bytes.of("test");
        byte[] hash = Hash.h256(data);

        assertFalse(Key.verify(hash, Bytes.random(20)));
        assertFalse(Key.verify(hash, Bytes.random(Signature.LENGTH)));
        assertFalse(Key.verify(hash, Bytes.random(200)));
    }

    @Test
    public void testSignatureSize() {
        Key key = new Key();
        byte[] data = Bytes.of("test");

        byte[] hash = Hash.h256(data);
        byte[] sig = key.sign(hash).toBytes();

        logger.info("signature size: {} B, {} GB per year", sig.length,
                64.0 * sig.length * Constants.BLOCKS_PER_DAY * 365 / 1024 / 1024 / 1024);
    }

    @Test
    public void testImportPrivateKeyDynamic() throws InvalidKeySpecException {
        KeyPairGenerator gen = new KeyPairGenerator();
        KeyPair keypair = gen.generateKeyPair();
        Key account = new Key(keypair.getPrivate().getEncoded());
        assertEquals(Hex.encode(keypair.getPublic().getEncoded()), Hex.encode(account.getPublicKey()));
    }

    @Test
    public void testImportPrivateKeyStatic() throws InvalidKeySpecException {
        Key account = new Key(Hex.decode(
                "302e020100300506032b657004220420bd2f24b259aac4bfce3792c31d0f62a7f28b439c3e4feb97050efe5fe254f2af"));
        assertEquals("302a300506032b6570032100b72dc8ebc9f53d21837dc96483da08765ea11f25c1bd4c3cb49318c944d67b9b",
                Hex.encode(account.getPublicKey()));
    }
}
