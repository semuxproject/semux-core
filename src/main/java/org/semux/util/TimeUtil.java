/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.time.Duration;

public class TimeUtil {

    private TimeUtil() {
    }

    /**
     * Returns a human-readable duration
     * 
     * @param duration
     * @return
     */
    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }
}
