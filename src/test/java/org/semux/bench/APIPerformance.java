/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.semux.api.SemuxAPIMock;
import org.semux.api.response.GetInfoResponse;
import org.semux.config.Config;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.rules.TemporaryDBRule;
import org.semux.util.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIPerformance {
    private static final Logger logger = LoggerFactory.getLogger(APIPerformance.class);

    private static final String API_IP = "127.0.0.1";
    private static final int API_PORT = 15171;

    private static int REPEAT = 1000;

    public static void testBasic() throws IOException {
        Wallet wallet = new Wallet(new File("wallet_test.data"));
        wallet.unlock("passw0rd");
        wallet.addAccount(new EdDSA());

        SemuxAPIMock api = new SemuxAPIMock(new TemporaryDBRule());
        api.start(API_IP, API_PORT);

        try {
            Config config = api.getKernel().getConfig();
            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {

                ApiClient a = new ApiClient(new InetSocketAddress(API_IP, API_PORT), config.apiUsername(),
                        config.apiPassword());
                a.request(GetInfoResponse.class, "get_info");
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
