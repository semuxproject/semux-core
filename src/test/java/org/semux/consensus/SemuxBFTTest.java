/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.config.MainNetConfig;
import org.semux.integration.KernelTestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxBFTTest {

    private static final Logger logger = LoggerFactory.getLogger(SemuxBFTTest.class);

    @Rule
    public KernelTestRule kernel = new KernelTestRule(51610, 51710);

    @Test
    public void testIsPrimary() {
        List<String> validators = Arrays.asList("a", "b", "c", "d");
        int blocks = 1000;
        int repeat = 0;
        int last = -1;

        SemuxBFT bft = mock(SemuxBFT.class);
        bft.config = new MainNetConfig(Constants.DEFAULT_DATA_DIR);
        bft.validators = validators;
        when(bft.isPrimary(anyLong(), anyInt(), anyString())).thenCallRealMethod();

        Random r = new Random(System.nanoTime());
        for (int i = 0; i < blocks; i++) {
            int view = r.nextInt(2);
            for (int j = 0; j < validators.size(); j++) {
                if (bft.isPrimary(i, view, validators.get(j))) {
                    if (j == last) {
                        repeat++;
                    }
                    last = j;
                }
            }
        }
        logger.info("Consecutive validator probability: {}/{}", repeat, blocks);
        assertEquals(1.0 / validators.size(), (double) repeat / blocks, 0.05);
    }
}
