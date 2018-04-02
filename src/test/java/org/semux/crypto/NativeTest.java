/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeTest {

    private static final Logger logger = LoggerFactory.getLogger(NativeTest.class);

    private static final byte[] MESSAGE = Bytes.of("test");
    private static final byte[] BLAKE2B_HASH = Hex
            .decode("928b20366943e2afd11ebc0eae2e53a93bf177a4fcf35bcc64d503704e65e202");
    private static final byte[] PUBLIC_KEY = Hex
            .decode("04dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private static final byte[] PRIVATE_KEY = Hex
            .decode("a73994d748b936ce63ae254508d909381fdbec3bbc11f4401630414f21fecb1704dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private static final byte[] ED25519_SIGNATURE = Hex
            .decode("cc52ae5b72af210073756f801cf8ffa36cefe96c5010b2cf25d04dfc5b0495e4ee3e14774c4607a4475f2b449a3181c9bd2c6aed46ed283debfebe19589f550e");

    @BeforeClass
    public static void setup() {
        Native.disable();
    }

    @AfterClass
    public static void teardown() {
        Native.enable();
    }

    @Test
    public void testBlake2bNull() {
        assertNull(Native.blake2b(null));
    }

    @Test
    public void testBlake2b() {
        assertArrayEquals(BLAKE2B_HASH, Native.blake2b(MESSAGE));
    }

    @Test
    public void testEd25519SignNull() {
        assertNull(Native.ed25519_sign(null, null));
    }

    @Test
    public void testEd25519Sign() {
        assertArrayEquals(ED25519_SIGNATURE, Native.ed25519_sign(MESSAGE, PRIVATE_KEY));
    }

    @Test
    public void testEd25519VerifyNull() {
        assertFalse(Native.ed25519_verify(null, null, null));
    }

    @Test
    public void testEd25519Verify() {
        assertTrue(Native.ed25519_verify(MESSAGE, ED25519_SIGNATURE, PUBLIC_KEY));
    }

    @Test
    public void testCompatibility() {
        Key key = new Key();
        Key.Signature sig = key.sign(MESSAGE);

        logger.debug("seed  : " + Hex.encode(key.sk.getSeed()));
        logger.debug("sk    : " + Hex.encode(key.sk.geta()));
        logger.debug("pk    : " + Hex.encode(key.pk.getAbyte()));
        logger.debug("sig   : " + Hex.encode(sig.getS()));
        logger.debug("--------------------");

        byte[] pk2 = Bytes.merge(key.sk.getSeed(), key.sk.getAbyte());
        byte[] sig2 = Native.ed25519_sign(MESSAGE, pk2);
        logger.debug("sk 2  : " + Hex.encode(pk2));
        logger.debug("sig 2 : " + Hex.encode(sig2));

        assertArrayEquals(sig.getS(), sig2);
    }

    @Ignore
    @Test
    public void benchmarkBlake2b() {
        byte[] data = Bytes.random(512);
        int repeat = 50_000;

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Hash.h256(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.blake2b(data);
        }
        Instant end = Instant.now();
        logger.debug("Blake2b native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Hash.h256(data);
        }
        end = Instant.now();
        logger.debug("Blake2b java: " + Duration.between(start, end).toMillis() + "ms");

        assertArrayEquals(Hash.h256(data), Native.blake2b(data));
    }

    @Ignore
    @Test
    public void benchmarkEd25519Sign() {
        byte[] data = Bytes.random(512);
        int repeat = 50_000;

        Key key = new Key();

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Native.ed25519_sign(data, PRIVATE_KEY);
            key.sign(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.ed25519_sign(data, PRIVATE_KEY);
        }
        Instant end = Instant.now();
        logger.debug("Ed25519 sign native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            key.sign(data);
        }
        end = Instant.now();
        logger.debug("Ed25519 sign java: " + Duration.between(start, end).toMillis() + "ms");
    }

    @Ignore
    @Test
    public void benchmarkEd25519Verify() {
        byte[] data = Bytes.random(512);
        int repeat = 50_000;

        Key key = new Key();
        Key.Signature sig = key.sign(data);

        byte[] sigNative = Native.ed25519_sign(data, PRIVATE_KEY);

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Native.ed25519_verify(data, sigNative, PUBLIC_KEY);
            key.verify(data, sig);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.ed25519_verify(data, sigNative, PUBLIC_KEY);
        }
        Instant end = Instant.now();
        logger.debug("Ed25519 verify native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            key.verify(data, sig);
        }
        end = Instant.now();
        logger.debug("Ed25519 verify java: " + Duration.between(start, end).toMillis() + "ms");
    }
}
