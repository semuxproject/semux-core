/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import org.junit.Assert;
import org.junit.Test;
import org.semux.Config;
import org.semux.crypto.EdDSA.Signature;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.KeyPairGenerator;

public class EdDSATest {

    private static final Logger logger = LoggerFactory.getLogger(EdDSATest.class);

    @Test
    public void testGenerateKeyPair() throws InvalidKeySpecException {
        EdDSA key1 = new EdDSA();

        assertEquals(44, key1.getPublicKey().length);
        assertEquals(48, key1.getPrivateKey().length);

        EdDSA key2 = new EdDSA(key1.getPublicKey(), key1.getPrivateKey());

        Assert.assertArrayEquals(key1.getPublicKey(), key2.getPublicKey());
        Assert.assertArrayEquals(key1.getPrivateKey(), key2.getPrivateKey());
    }

    @Test
    public void testSignAndVerify() throws SignatureException {
        EdDSA key = new EdDSA();
        byte[] data = Bytes.of("test");

        byte[] hash = Hash.h256(data);
        byte[] sig = key.sign(hash).toBytes();

        assertTrue(EdDSA.verify(hash, sig));
        assertArrayEquals(key.getPublicKey(), Signature.fromBytes(sig).getPublicKey());
        assertArrayEquals(key.toAddress(), Signature.fromBytes(sig).getAddress());
    }

    @Test
    public void testSignLargeData() throws SignatureException {
        byte[] data = Bytes.random(1024 * 1024);

        EdDSA key = new EdDSA();
        Signature sig = key.sign(data);

        assertTrue(EdDSA.verify(data, sig));
        assertArrayEquals(key.getPublicKey(), sig.getPublicKey());
    }

    @Test
    public void testInvalidSignature() throws SignatureException {
        byte[] data = Bytes.of("test");
        byte[] hash = Hash.h256(data);

        assertFalse(EdDSA.verify(hash, Bytes.random(20)));
        assertFalse(EdDSA.verify(hash, Bytes.random(200)));
    }

    @Test
    public void testSignatureSize() {
        EdDSA key = new EdDSA();
        byte[] data = Bytes.of("test");

        byte[] hash = Hash.h256(data);
        byte[] sig = key.sign(hash).toBytes();

        logger.info("signature size: {} B, {} GB per year", sig.length,
                64.0 * sig.length * Config.BLOCKS_PER_DAY * 365 / 1024 / 1024 / 1024);
    }

    @Test
    public void testImportPrivateKeyDynamic() throws InvalidKeySpecException {
        KeyPairGenerator gen = new KeyPairGenerator();
        KeyPair keypair = gen.generateKeyPair();
        EdDSA account = new EdDSA(keypair.getPrivate().getEncoded());
        assertEquals(Hex.encode(keypair.getPublic().getEncoded()), Hex.encode(account.getPublicKey()));
    }

    @Test
    public void testImportPrivateKeyStatic() throws InvalidKeySpecException {
        EdDSA account = new EdDSA(Hex.decode(
                "302e020100300506032b657004220420bd2f24b259aac4bfce3792c31d0f62a7f28b439c3e4feb97050efe5fe254f2af"));
        assertEquals("302a300506032b6570032100b72dc8ebc9f53d21837dc96483da08765ea11f25c1bd4c3cb49318c944d67b9b",
                Hex.encode(account.getPublicKey()));
    }
}
