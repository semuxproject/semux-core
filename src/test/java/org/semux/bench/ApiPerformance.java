/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Rule;
import org.junit.Test;
import org.semux.api.SemuxApiMock;
import org.semux.config.Config;
import org.semux.rules.KernelRule;
import org.semux.util.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: investigate, significant performance decrease noticed.
 */
public class ApiPerformance {
    private static final Logger logger = LoggerFactory.getLogger(ApiPerformance.class);

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    @Test
    public void testBasic() throws IOException {
        SemuxApiMock api = new SemuxApiMock(kernelRule.getKernel());
        api.start();

        try {
            int repeat = 1000;

            Config config = api.getKernel().getConfig();
            long t1 = System.nanoTime();
            for (int i = 0; i < repeat; i++) {
                ApiClient a = new ApiClient(new InetSocketAddress(config.apiListenIp(), config.apiListenPort()),
                        config.apiUsername(),
                        config.apiPassword());
                a.request("get_info");
            }
            long t2 = System.nanoTime();
            logger.info("Perf_api_basic: " + (t2 - t1) / 1_000 / repeat + " μs/time");
        } finally {
            api.stop();
        }
    }
}
