/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.Config;
import org.semux.api.APIServerMock;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;

public class ApiUtilTest {
    private static File file = new File("wallet_test.data");
    private static String password = "password";

    private static Wallet wallet;
    private static APIServerMock api;

    @BeforeClass
    public static void setup() {
        Config.init();

        wallet = new Wallet(file);
        wallet.unlock(password);
        wallet.addAccount(new EdDSA());

        api = new APIServerMock();
        api.start(wallet, Config.API_LISTEN_IP, Config.API_LISTEN_PORT);
    }

    @Test
    public void testRequest() throws IOException {
        String cmd = "get_block";

        ApiUtil api = new ApiUtil(new InetSocketAddress("127.0.0.1", 5171), Config.API_USERNAME, Config.API_PASSWORD);
        JSONObject obj = api.request(cmd, "number", 0);

        assertTrue(obj.getBoolean("success"));
        assertNotNull(obj.getJSONObject("result"));
    }

    @AfterClass
    public static void teardown() throws IOException {
        api.stop();
        if (wallet.exists()) {
            wallet.delete();
        }
    }
}
