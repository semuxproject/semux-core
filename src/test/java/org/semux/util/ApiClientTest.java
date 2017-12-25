/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.semux.api.SemuxAPIMock;
import org.semux.rules.KernelRule;

public class ApiClientTest {

    @ClassRule
    public static KernelRule kernelRule = new KernelRule(51610, 51710);

    private SemuxAPIMock api;

    @Before
    public void setup() {
        api = new SemuxAPIMock(kernelRule.getKernel());
        api.start();
    }

    @After
    public void teardown() {
        api.stop();
    }

    @Test
    public void testRequest() throws IOException {
        String cmd = "get_block";

        ApiClient apiClient = kernelRule.getKernel().getApiClient();
        String response = apiClient.request(cmd, "number", 0);

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("result"));
    }
}