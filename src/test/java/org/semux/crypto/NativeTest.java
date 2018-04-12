/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeTest {

    private static final Logger logger = LoggerFactory.getLogger(NativeTest.class);

    private static final byte[] MESSAGE = Bytes.of("test");
    private static final byte[] H256 = Hex
            .decode("928b20366943e2afd11ebc0eae2e53a93bf177a4fcf35bcc64d503704e65e202");
    private static final byte[] H160 = Hex
            .decode("86e8402b7615f07a2acb2ef1f4a54d323bbede77");
    private static final byte[] PUBLIC_KEY = Hex
            .decode("04dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private static final byte[] PRIVATE_KEY = Hex
            .decode("a73994d748b936ce63ae254508d909381fdbec3bbc11f4401630414f21fecb1704dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private static final byte[] SIGNATURE = Hex
            .decode("cc52ae5b72af210073756f801cf8ffa36cefe96c5010b2cf25d04dfc5b0495e4ee3e14774c4607a4475f2b449a3181c9bd2c6aed46ed283debfebe19589f550e");

    @BeforeClass
    public static void setup() {
        assertTrue(Native.isEnabled());
        Native.disable();
    }

    @AfterClass
    public static void teardown() {
        Native.enable();
    }

    @Test(expected = CryptoException.class)
    public void testH256Null() {
        Native.h256(null);
    }

    @Test
    public void testH256() {
        assertArrayEquals(H256, Native.h256(MESSAGE));
    }

    @Test(expected = CryptoException.class)
    public void testH160Null() {
        Native.h160(null);
    }

    @Test
    public void testH160() {
        assertArrayEquals(H160, Native.h160(MESSAGE));
    }

    @Test(expected = CryptoException.class)
    public void testSignNull() {
        Native.sign(null, null);
    }

    @Test
    public void testSign() {
        assertArrayEquals(SIGNATURE, Native.sign(MESSAGE, PRIVATE_KEY));
    }

    @Test
    public void testVerifyNull() {
        assertFalse(Native.verify(null, null, null));
    }

    @Test
    public void testVerify() {
        assertTrue(Native.verify(MESSAGE, SIGNATURE, PUBLIC_KEY));
    }

    @Test
    public void testCompatibility() {
        assertArrayEquals(Native.h256(MESSAGE), Hash.h256(MESSAGE));
        assertArrayEquals(Native.h160(MESSAGE), Hash.h160(MESSAGE));

        Key key = new Key();
        Key.Signature sig = key.sign(MESSAGE);

        byte[] pk2 = Bytes.merge(key.sk.getSeed(), key.sk.getAbyte());
        byte[] sig2 = Native.sign(MESSAGE, pk2);

        assertArrayEquals(sig.getS(), sig2);
    }

    @Test
    public void benchmarkH256() {
        byte[] data = Bytes.random(512);
        int repeat = 20_000;

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Hash.h256(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.h256(data);
        }
        Instant end = Instant.now();
        logger.debug("H256 Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Hash.h256(data);
        }
        end = Instant.now();
        logger.debug("H256 Java: " + Duration.between(start, end).toMillis() + "ms");

        assertArrayEquals(Hash.h256(data), Native.h256(data));
    }

    @Test
    public void benchmarkH160() {
        byte[] data = Bytes.random(512); // typical input
        int repeat = 20_000;

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Hash.h160(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.h160(data);
        }
        Instant end = Instant.now();
        logger.debug("H160 Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Hash.h160(data);
        }
        end = Instant.now();
        logger.debug("H160 Java: " + Duration.between(start, end).toMillis() + "ms");

        assertArrayEquals(Hash.h256(data), Native.h256(data));
    }

    @Test
    public void benchmarkSign() {
        byte[] data = Bytes.random(512);
        int repeat = 20_000;

        Key key = new Key();

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Native.sign(data, PRIVATE_KEY);
            key.sign(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.sign(data, PRIVATE_KEY);
        }
        Instant end = Instant.now();
        logger.debug("Sign Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            key.sign(data);
        }
        end = Instant.now();
        logger.debug("Sign Java: " + Duration.between(start, end).toMillis() + "ms");
    }

    @Test
    public void benchmarkVerify() {
        byte[] data = Bytes.random(512);
        int repeat = 20_000;

        Key key = new Key();
        Key.Signature sig = key.sign(data);

        byte[] sigNative = Native.sign(data, PRIVATE_KEY);

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Native.verify(data, sigNative, PUBLIC_KEY);
            Key.verify(data, sig);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.verify(data, sigNative, PUBLIC_KEY);
        }
        Instant end = Instant.now();
        logger.debug("Verify Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Key.verify(data, sig);
        }
        end = Instant.now();
        logger.debug("Verify Java: " + Duration.between(start, end).toMillis() + "ms");
    }
}
