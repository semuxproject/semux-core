/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.io.File;
import java.io.IOException;

import org.semux.Config;
import org.semux.api.APIServerMock;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.util.ApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIPerformance {
    private static final Logger logger = LoggerFactory.getLogger(APIPerformance.class);

    private static int REPEAT = 1000;

    public static void testBasic() throws IOException {
        Wallet wallet = new Wallet(new File("wallet_test.data"));
        wallet.unlock("passw0rd");
        wallet.addAccount(new EdDSA());

        APIServerMock api = new APIServerMock();
        api.start(wallet, Config.API_LISTEN_IP, Config.API_LISTEN_PORT);

        try {
            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                ApiUtil a = new ApiUtil(Config.API_USERNAME, Config.API_PASSWORD);
                a.request("get_info");
            }
            long t2 = System.nanoTime();
            logger.info("Perf_api_basic: " + (t2 - t1) / 1_000 / REPEAT + " μs/time");
        } finally {
            api.stop();
        }
    }

    public static void main(String[] args) throws Exception {
        testBasic();
    }
}
