/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections4.map.LRUMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CacheTest {

    private static final Logger logger = LoggerFactory.getLogger(CacheTest.class);

    @Test
    public void testPerformance() {
        Map<Integer, Integer> map = new LRUMap<>();
        Map<Integer, Integer> mapSync = Collections.synchronizedMap(new LRUMap<>());
        Cache<Integer, Integer> cache = Caffeine.newBuilder().build();

        // warm up
        int repeat = 1000;
        Random r = new Random();
        int bound = repeat * 10;
        for (int i = 0; i < repeat; i++) {
            map.put(r.nextInt(bound), r.nextInt(bound));
            mapSync.put(r.nextInt(bound), r.nextInt(bound));
            cache.put(r.nextInt(bound), r.nextInt(bound));

            map.get(r.nextInt(bound));
            mapSync.get(r.nextInt(bound));
            cache.getIfPresent(r.nextInt(bound));
        }

        // write
        long t1 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            map.put(r.nextInt(bound), r.nextInt(bound));
        }
        long t2 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            mapSync.put(r.nextInt(bound), r.nextInt(bound));
        }
        long t3 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            cache.put(r.nextInt(bound), r.nextInt(bound));
        }
        long t4 = System.nanoTime();
        logger.info("Write: LRUMap = {} ns, LRUMap (synchronized) = {} ns, Caffeine = {} ns", t2 - t1, t3 - t2,
                t4 - t3);

        // read
        t1 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            map.get(r.nextInt(bound));
        }
        t2 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            mapSync.get(r.nextInt(bound));
        }
        t3 = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            cache.getIfPresent(r.nextInt(bound));
        }
        t4 = System.nanoTime();
        logger.info("Read: LRUMap = {} ns, LRUMap (synchronized) = {} ns, Caffeine = {} ns", t2 - t1, t3 - t2,
                t4 - t3);
    }

}
