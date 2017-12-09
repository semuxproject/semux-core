/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.semux.api.SemuxAPIMock;
import org.semux.api.response.GetBlockResponse;
import org.semux.config.Config;
import org.semux.rules.TemporaryDBRule;

public class ApiUtilTest {

    @ClassRule
    public static TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    private static final String API_IP = "127.0.0.1";
    private static final int API_PORT = 15171;

    private static SemuxAPIMock api;

    @BeforeClass
    public static void setup() {
        api = new SemuxAPIMock(temporaryDBFactory);
        api.start(API_IP, API_PORT);
    }

    @Test
    public void testRequest() throws IOException {
        String cmd = "get_block";

        Config config = api.getKernel().getConfig();
        ApiClient api = new ApiClient(new InetSocketAddress(API_IP, API_PORT), config.apiUsername(),
                config.apiPassword());
        GetBlockResponse response = api.request(GetBlockResponse.class, cmd, "number", 0);

        assertTrue(response.success);
        assertNotNull(response.block);
    }

    @AfterClass
    public static void teardown() throws IOException {
        api.stop();
    }
}