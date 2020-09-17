/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import org.semux.crypto.Hash;
import org.semux.crypto.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoPerformance {
    private static final Logger logger = LoggerFactory.getLogger(CryptoPerformance.class);

    private static int[] DATA_SIZES = { 1024, 1024 * 1024 };
    private static int REPEAT = 1000;

    public static void testH256() {
        for (int size : DATA_SIZES) {
            byte[] data = new byte[size];

            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                Hash.h256_s(data, null);
            }
            long t2 = System.nanoTime();

            logger.info("Perf_h256_{}k: {} μs/time", size / 1024, (t2 - t1) / 1_000 / REPEAT);
        }
    }

    public static void testH160() {
        for (int size : DATA_SIZES) {
            byte[] data = new byte[size];

            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                Hash.h160(data);
            }
            long t2 = System.nanoTime();

            logger.info("Perf_h160_{}k: {} μs/time", size / 1024, (t2 - t1) / 1_000 / REPEAT);
        }
    }

    public static void testSign() {
        for (int size : DATA_SIZES) {
            Key eckey = new Key();
            byte[] data = new byte[size];
            byte[] hash = Hash.h256_s(data, null);

            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                eckey.sign(hash);
            }
            long t2 = System.nanoTime();

            logger.info("Perf_sign_{}k: {} μs/time", size / 1024, (t2 - t1) / 1_000 / REPEAT);
        }
    }

    public static void testVerify() {
        for (int size : DATA_SIZES) {
            Key eckey = new Key();
            byte[] data = new byte[size];
            byte[] hash = Hash.h256_s(data, null);
            byte[] sig = eckey.sign(hash).toBytes();

            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                Key.verify(hash, sig);
            }
            long t2 = System.nanoTime();

            logger.info("Perf_verify_{}k: {} μs/time", size / 1024, (t2 - t1) / 1_000 / REPEAT);
        }
    }

    public static void main(String[] args) throws Exception {
        testH256();
        testH160();
        testSign();
        testVerify();
    }
}
