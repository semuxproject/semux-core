/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeUtil {

    private static final Logger logger = LoggerFactory.getLogger(TimeUtil.class);
    private static final ThreadFactory factory = new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ntpUpdate-" + cnt.getAndIncrement());
        }
    };

    public static final String DEFAULT_DURATION_FORMAT = "%02d:%02d:%02d";
    public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String NTP_POOL = "pool.ntp.org";
    private static final ScheduledExecutorService ntpUpdateTimer = Executors.newSingleThreadScheduledExecutor(factory);
    private static final int TIME_RETRIES = 5;

    private static long timeOffsetFromNtp = 0;

    static {
        // inline run at start
        updateNetworkTimeOffset();
        // update time every hour
        ntpUpdateTimer.scheduleAtFixedRate(TimeUtil::updateNetworkTimeOffset, 60 * 60 * 1000, 60, TimeUnit.MINUTES);
    }

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
     * @param timestamp
     *            timestamp in milliseconds to be formatter
     * @return formatted timestamp in yyyy-MM-dd HH:mm:ss
     */
    public static String formatTimestamp(Long timestamp) {
        return new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT).format(new Date(timestamp));
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis() + timeOffsetFromNtp;
    }

    /**
     * Update time offset from NTP
     */
    private static void updateNetworkTimeOffset() {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(10000);
        for (int i = 0; i < TIME_RETRIES; i++) {
            try {
                client.open();
                InetAddress hostAddr = InetAddress.getByName(NTP_POOL);
                TimeInfo info = client.getTime(hostAddr);
                info.computeDetails();

                // update our current internal state
                timeOffsetFromNtp = info.getOffset();
                // break from retry loop
                return;
            } catch (IOException e) {
                logger.warn("Unable to retrieve NTP time");
            } finally {
                client.close();
            }
        }
    }

    public static long getTimeOffsetFromNtp() {
        return timeOffsetFromNtp;
    }

    public static void shutdownNtpUpdater() {
        ntpUpdateTimer.shutdownNow();
    }

    private TimeUtil() {
    }
}
