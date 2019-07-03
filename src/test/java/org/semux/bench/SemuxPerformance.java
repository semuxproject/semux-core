/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import static org.semux.core.Amount.Unit.MILLI_SEM;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.util.Bytes;
import org.semux.util.ConsoleUtil;
import org.semux.util.SimpleApiClient;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxPerformance {

    private static final Logger logger = LoggerFactory.getLogger(SemuxPerformance.class);

    private static String host = "127.0.0.1";
    private static int port = 5171;
    private static String username = "";
    private static String password = "";

    private static String address = "";
    private static int tps = 500;

    public static void testTransfer(int n) throws IOException, InterruptedException {
        DevnetConfig config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);

        long t1 = TimeUtil.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("from", address);
            params.put("to", address);
            params.put("value", MILLI_SEM.of(1).getNano());
            params.put("fee", config.spec().minTransactionFee().getNano());
            params.put("data", Bytes.EMPTY_BYTES);
            params.put("password", password);

            SimpleApiClient api = new SimpleApiClient(host, port, username, password);
            String response = api.post("/transaction/transfer", params);
            if (!response.contains("\"success\":true")) {
                logger.info(response);
                return;
            }

            if (i % tps == 0) {
                logger.info(new SimpleDateFormat("[HH:mm:ss]").format(new Date()) + " " + i);
                long t2 = TimeUtil.currentTimeMillis();
                Thread.sleep(Math.max(0, 1000 - (t2 - t1)));
                t1 = t2;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        address = ConsoleUtil.readPassword("Please enter your wallet address: ");
        username = ConsoleUtil.readPassword("Please enter your API username: ");
        password = ConsoleUtil.readPassword("Please enter your API password: ");

        while (true) {
            int n = Integer.parseInt(ConsoleUtil.readLine("# transactions to send: ").replaceAll("[^\\d]", ""));
            if (n > 0) {
                testTransfer(n);
            } else {
                break;
            }
        }
    }
}
