/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v1_1_0;

import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.semux.api.v1_1_0.model.ApiHandlerResponse;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * The test case covers validation rules of
 * {@link org.semux.api.v1_1_0.impl.ApiHandlerImpl}
 */
@RunWith(Parameterized.class)
public class SemuxApiErrorTest extends SemuxApiTestBase {

    private static final String ADDRESS_PLACEHOLDER = "[wallet]";

    @Parameters(name = "request(\"{0}\")")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "/add_node" },
                { "/add_node?node=I_am_not_a_node" },
                { "/add_node?node=127.0.0.1:65536" },
                { "/add_node?node=.com:5161" },
                { "/add_to_blacklist" },
                { "/add_to_blacklist?ip=I_am_not_an_IP" },
                { "/add_to_whitelist" },
                { "/add_to_whitelist?ip=I_am_not_an_IP" },
                { "/get_block_by_number" },
                { "/get_block_by_number?number=9999999999999999" },
                { "/get_account" },
                { "/get_account?address=0xabc" },
                { "/get_account?address=I_am_not_an_address" },
                { "/get_delegate" },
                { "/get_delegate?address=" + Hex.encode(Bytes.random(20)) },
                { "/get_delegate?address=I_am_not_an_address" },
                { "/get_account_transactions" },
                { "/get_account_transactions?address=I_am_not_an_address" },
                { format("/get_account_transactions?address=%s", randomHex()) },
                { format("/get_account_transactions?address=%s&from=%s", randomHex(), "I_am_not_a_number") },
                { format("/get_account_transactions?address=%s&from=%s&to=%s", randomHex(), "0", "I_am_not_a_number") },
                { "/get_transaction" },
                { format("/get_transaction?hash=%s", "I_am_not_a_hexadecimal_string") },
                { format("/get_transaction?hash=%s", randomHex()) },
                { "/send_transaction" },
                { "/send_transaction?raw=I_am_not_a_hexadecimal_string" },
                { "/get_vote" },
                { format("/get_vote?voter=%s", "I_am_not_a_valid_address") },
                { format("/get_vote?voter=%s", randomHex()) },
                { format("/get_vote?voter=%s&delegate=%s", randomHex(), "I_am_not_a_valid_address") },
                { "/get_votes" },
                { "/get_votes?delegate=I_am_not_hexadecimal_string" },
                { "/transfer" },
                { format("/transfer?from=%s", "_") }, // non-hexadecimal address
                { format("/transfer?from=%s", randomHex()) }, // non wallet address
                { format("/transfer?from=%s", ADDRESS_PLACEHOLDER) },
                { format("/transfer?from=%s&to=%s", ADDRESS_PLACEHOLDER, "_") }, // non-hexadecimal to
                { format("/transfer?from=%s&to=%s", ADDRESS_PLACEHOLDER, randomHex()) },
                { format("/transfer?from=%s&to=%s&value=%s", ADDRESS_PLACEHOLDER, randomHex(), "_") }, // non-number
                { format("/transfer?from=%s&to=%s&value=%s", ADDRESS_PLACEHOLDER, randomHex(), "10") },
                { format("/transfer?from=%s&to=%s&value=%s&fee=%s", ADDRESS_PLACEHOLDER, randomHex(), "10", "_") }, // non-number
                { format("/transfer?from=%s&to=%s&value=%s&fee=%s", ADDRESS_PLACEHOLDER, randomHex(), "10", "10") },
                { format("/transfer?from=%s&to=%s&value=%s&fee=%s&data=%s", ADDRESS_PLACEHOLDER, randomHex(), "10",
                        "10", "_") }, // non-hexadecimal data
                { format("/transfer?from=%s&to=%s&value=%s&fee=%s&data=%s", ADDRESS_PLACEHOLDER, randomHex(), "10",
                        "10", randomHex()) }, // hexadecimal data
                { "/get_transaction_limits" },
                { "/get_transaction_limits?type=XXX" },
        });
    }

    @Parameter
    public String uri;

    @Before
    public void setUp() {
        super.setUp();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testError() throws IOException {
        uri = uri.replace(ADDRESS_PLACEHOLDER, wallet.getAccount(0).toAddressString());

        WebClient webClient = WebClient.create(
                String.format("http://%s:%d/v1.1%s", config.apiListenIp(), config.apiListenPort(), uri),
                Collections.singletonList(new JacksonJsonProvider()),
                config.apiUsername(),
                config.apiPassword(),
                null);
        Response response = webClient.get();
        assertNotNull(response);
        ;
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ApiHandlerResponse apiHandlerResponse = response.readEntity(ApiHandlerResponse.class);
        assertNotNull(apiHandlerResponse);
        assertNotNull(apiHandlerResponse.getMessage());
        assertFalse(apiHandlerResponse.isSuccess());
        System.out.println(apiHandlerResponse.getMessage());
    }

    private static String randomHex() {
        return Hex.encode0x(Bytes.random(20));
    }
}