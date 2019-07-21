/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import static org.semux.core.Unit.MILLI_SEM;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.core.Amount;
import org.semux.crypto.Hex;
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

    private static int tps = 500;
    private static String type = "transfer";
    private static String from = "";
    private static String to = "";

    public static void testTransfer(int n) throws IOException, InterruptedException {
        DevnetConfig config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);

        long t1 = TimeUtil.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("from", from);
            params.put("to", to);
            params.put("value", Amount.of(1, MILLI_SEM).toString());
            params.put("fee", config.spec().minTransactionFee().toString());
            params.put("data", Bytes.EMPTY_BYTES);

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

    // https://github.com/ConsenSys/Tokens

    public static void testCall(int n) throws IOException, InterruptedException {
        long t1 = TimeUtil.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            byte[] data = Bytes.merge(
                    Hex.decode0x("0xa9059cbb"), // keccak256("transfer(address,uint256)")
                    new byte[12], Bytes.random(20), // address
                    new byte[31], new byte[] { 1 } // uint256
            );

            Map<String, Object> params = new HashMap<>();
            params.put("from", from);
            params.put("to", to);
            params.put("value", "0");
            params.put("data", Hex.encode0x(data));
            params.put("gas", "40000");
            params.put("gasPrice", "1");

            SimpleApiClient api = new SimpleApiClient(host, port, username, password);
            String response = api.post("/transaction/call", params);
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
        username = ConsoleUtil.readPassword("Please enter your API username: ");
        password = ConsoleUtil.readPassword("Please enter your API password: ");

        tps = Integer.parseInt(ConsoleUtil.readLine("Transaction throughput (tx/s): ").trim());
        type = ConsoleUtil.readLine("Transaction type: ").trim().toLowerCase();
        from = ConsoleUtil.readLine("From address: ").trim();
        to = ConsoleUtil.readLine("To address: ").trim();

        while (true) {
            int n = Integer.parseInt(ConsoleUtil.readLine("# transactions to send: ").trim());
            if (n > 0) {
                switch (type) {
                case "transfer":
                    testTransfer(n);
                    break;
                case "call":
                    testCall(n);
                    break;
                }
            } else {
                break;
            }
        }
    }
}
