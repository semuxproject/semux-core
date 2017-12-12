/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.core.Unit;
import org.semux.util.ApiClient;
import org.semux.util.Bytes;
import org.semux.util.SystemUtil;

public class SemuxPerformance {
    private static InetSocketAddress server = new InetSocketAddress("127.0.0.1", 5171);
    private static String username = "";
    private static String password = "";

    private static String address = "";
    private static int tps = 500;

    public static void testTransfer(int n) throws IOException, InterruptedException {
        DevNetConfig config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);

        long t1 = System.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("from", address);
            params.put("to", address);
            params.put("value", 1 * Unit.MILLI_SEM);
            params.put("fee", config.minTransactionFee());
            params.put("data", Bytes.EMPTY_BYTES);
            params.put("password", password);

            ApiClient api = new ApiClient(server, username, password);
            String response = api.request("transfer", params);
            if (!response.contains("\"success\":true")) {
                System.out.println(response);
                return;
            }

            if (i % tps == 0) {
                System.out.println(new SimpleDateFormat("[HH:mm:ss]").format(new Date()) + " " + i);
                long t2 = System.currentTimeMillis();
                Thread.sleep(Math.max(0, 1000 - (t2 - t1)));
                t1 = t2;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        address = SystemUtil.readPassword("Please enter your wallet address: ");
        username = SystemUtil.readPassword("Please enter your username: ");
        password = SystemUtil.readPassword();

        while (true) {
            System.out.print("# transactions to send: ");
            System.out.flush();

            int n = Integer.parseInt(SystemUtil.SCANNER.nextLine().replaceAll("[^\\d]", ""));
            if (n > 0) {
                testTransfer(n);
            } else {
                break;
            }
        }
    }
}