/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import org.semux.crypto.EdDSA;
import org.semux.crypto.Hash;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

public class CryptoLargePerformance {
    private static final Logger logger = LoggerFactory.getLogger(CryptoLargePerformance.class);

    private static final long TIMES = 10000;
    private static final int SIZE = 1024 * 1024; // 1MB

    /**
     * Verify 1 MB of data for 10000 times (~ 10 GB in total)
     */
    public static void testVerifyLarge() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        Instant t1 = Instant.now();

        Random random = new Random();
        for (int i = 1; i <= TIMES; i++) {
            EdDSA eckey = new EdDSA();
            byte[] data = new byte[SIZE];
            random.nextBytes(data);
            byte[] hash = Hash.h256(data);
            byte[] sig = eckey.sign(hash).toBytes();

            if (i % 1000 == 0) {
                System.out.printf("...%d\n", i);
            }

            EdDSA.verify(hash, sig);
        }

        Instant t2 = Instant.now();
        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();

        logger.info("Perf_verify_large: {} bytes, took {}", usedMemoryAfter - usedMemoryBefore,
                TimeUtil.formatDuration(Duration.between(t1, t2)));
    }

    public static void main(String[] args) throws Exception {
        testVerifyLarge();
    }
}
