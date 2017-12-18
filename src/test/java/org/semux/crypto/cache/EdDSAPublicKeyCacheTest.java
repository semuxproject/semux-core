/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file LICENSE or
 * https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.cache;

import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.semux.crypto.EdDSA;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

public class EdDSAPublicKeyCacheTest {

    @Test
    public void testCache() {
        EdDSA key = new EdDSA();
        byte[] b1 = key.getPublicKey().clone();
        byte[] b2 = key.getPublicKey().clone();

        EdDSAPublicKey p1 = EdDSAPublicKeyCache.computeIfAbsent(b1);
        EdDSAPublicKey p2 = EdDSAPublicKeyCache.computeIfAbsent(b2);
        assertTrue(b1 != b2);
        assertTrue(p1 == p2);
    }

    @Test
    @Ignore
    public void testJvmUtilization() {
        // RESULTS:
        // Soft value based cache is no long good for us, since we automatically allocate 80% of free
        // memory. The JVM will be very aggressive and eats a lot of memory.

        for (int i = 0; i < 1_000_000; i++) {
            EdDSAPublicKeyCache.computeIfAbsent(new EdDSA().getPublicKey());
            if (i % 10_000 == 0) {
                System.out.println(Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB");
            }
        }
    }
}
