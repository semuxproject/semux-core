/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TimeUtilTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { Duration.ofSeconds(86400), "24:00:00" },
                { Duration.ofSeconds(86400 - 1), "23:59:59" }, { Duration.ofSeconds(0), "00:00:00" },
                { Duration.ofSeconds(-1), "00:00:-1" } });
    }

    Duration duration;
    String formatted;

    public TimeUtilTest(Duration duration, String formatted) {
        this.duration = duration;
        this.formatted = formatted;
    }

    @Test
    public void testFormatDuration() {
        assertTrue(TimeUtil.formatDuration(duration).equals(formatted));
    }
}
