/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.api.SemuxApiMock;
import org.semux.crypto.Key;
import org.semux.rules.KernelRule;

public class SimpleApiClientTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    private SemuxApiMock api;

    @Before
    public void setUp() {
        api = new SemuxApiMock(kernelRule.getKernel());
        api.start();
    }

    @After
    public void tearDown() {
        api.stop();
    }

    @Test
    public void testGet() throws IOException {
        String uri = "/block-by-number";

        SimpleApiClient apiClient = kernelRule.getKernel().getApiClient();
        String response = apiClient.get(uri, "number", "0");

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("result"));
    }

    @Test
    public void testPost() throws IOException {
        Key key = new Key();
        String uri = "/account";

        SimpleApiClient apiClient = kernelRule.getKernel().getApiClient();
        String response = apiClient.post(uri, "name", "test", "privateKey", key.getPrivateKey());

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains(key.toAddressString()));
    }

    @Test
    public void testDelete() throws IOException {
        // prepare
        Key key = new Key();
        kernelRule.getKernel().getApiClient().post("/account", "name", "test", "privateKey", key.getPrivateKey());

        String uri = "/account";

        SimpleApiClient apiClient = kernelRule.getKernel().getApiClient();
        String response = apiClient.delete(uri, "address", key.toAddress());

        assertTrue(response.contains("\"success\":true"));
    }
}