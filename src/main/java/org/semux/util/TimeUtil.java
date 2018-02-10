/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class TimeUtil {

    public static final String DEFAULT_DURATION_FORMAT = "%02d:%02d:%02d";
    public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Returns a human-readable duration
     * 
     * @param duration
     *            duration object to be formatted
     * @return formatted duration in 00:00:00
     */
    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        return String.format(DEFAULT_DURATION_FORMAT, seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }

    /**
     *
     * @param timestamp
     *            timestamp in milliseconds to be formatter
     * @return formatted timestamp in yyyy-MM-dd HH:mm:ss
     */
    public static String formatTimestamp(Long timestamp) {
        return new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT).format(new Date(timestamp));
    }

    private TimeUtil() {
    }
}
