/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.cache;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Ignore;
import org.junit.Test;
import org.semux.crypto.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

public class PublicKeyCacheTest {

    private static final Logger logger = LoggerFactory.getLogger(PublicKeyCacheTest.class);

    @Test
    public void testCache() {
        Key key = new Key();
        byte[] b1 = key.getPublicKey().clone();
        byte[] b2 = key.getPublicKey().clone();

        EdDSAPublicKey p1 = PublicKeyCache.computeIfAbsent(b1);
        EdDSAPublicKey p2 = PublicKeyCache.computeIfAbsent(b2);
        assertNotSame(b1, b2);
        assertSame(p1, p2);
    }

    @Test
    @Ignore
    public void testJvmUtilization() {
        // RESULTS:
        // Soft value based cache is no long good for us, since we automatically
        // allocate 80% of free memory. The JVM will be very aggressive and eats a lot
        // of memory.

        for (int i = 0; i < 1_000_000; i++) {
            PublicKeyCache.computeIfAbsent(new Key().getPublicKey());
            if (i % 10_000 == 0) {
                logger.info(Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB");
            }
        }
    }
}
