/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.Amount.ZERO;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.semux.Network;
import org.semux.core.Amount;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainnetConfigTest {

    private Logger logger = LoggerFactory.getLogger(MainnetConfigTest.class);

    private MainnetConfig config;

    @Before
    public void testLoad() {
        config = new MainnetConfig(Constants.DEFAULT_DATA_DIR);
        assertEquals(Network.MAINNET, config.network());
    }

    @Test
    public void testBlockReward() {
        Amount total = LongStream
                .rangeClosed(1, 20_000_000)
                .mapToObj(config::getBlockReward)
                .reduce(ZERO, Amount::sum);

        assertEquals(SEM.of(22_000_000), total);
    }

    @Test
    public void testNumberOfValidators() {
        int last = 0;
        for (int i = 0; i < 60 * Constants.BLOCKS_PER_DAY; i++) {
            int n = config.getNumberOfValidators(i);
            if (n != last) {
                assertTrue(n > last && (n - last == 1 || last == 0));
                logger.info("block # = {}, validators = {}", i, n);
                last = n;
            }
        }

        assertEquals(100, last);
    }

    @Test
    public void testPrimaryUniformDistDeterminism() throws IOException {
        List<String> validators = IntStream.range(0, 100).boxed().map(i -> String.format("v%d", i))
                .collect(Collectors.toList());
        final int blocks = 1000;
        final int views = 10;
        int repeats = 0;

        String[][] primaryValidators = new String[blocks][views];

        MainnetConfig config = new MainnetConfig(Constants.DEFAULT_DATA_DIR);
        StringBuilder validatorsCSV = new StringBuilder();
        for (long i = 0; i < blocks; i++) {
            for (int view = 0; view < views; view++) {
                String primary = config.getPrimaryValidator(validators, i, view, true);
                primaryValidators[(int) i][view] = primary;

                if (view > 0 && primaryValidators[(int) i][view].equals(primaryValidators[(int) i][view - 1])) {
                    repeats++;
                }
            }

            validatorsCSV.append(StringUtils.join(primaryValidators[(int) i], ",")).append("\n");
        }

        logger.info("Repeats {} / {} = {}%", repeats, blocks, (double) repeats / (double) blocks * 100.0);

        assertEquals(0, repeats);
        assertEquals(
                FileUtils.readFileToString(
                        new File(MainnetConfigTest.class.getResource("/config/validators1000.csv").getFile()),
                        Charset.forName("UTF-8")).trim(),
                validatorsCSV.toString().trim());
    }

    @Test
    public void testWindowsLocalizedDatabaseDir() throws IOException {
        assumeTrue(SystemUtil.getOsName().equals(SystemUtil.OsName.WINDOWS));
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        assertThat(config.databaseDir().getCanonicalPath(), containsString("database\\mainnet"));
        Locale.setDefault(defaultLocale); // reset
    }
}
