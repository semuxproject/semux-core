/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v2;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.RandomUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.semux.api.ApiVersion;
import org.semux.api.v2.model.ApiHandlerResponse;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * The test case covers validation rules of {@link SemuxApiImpl}
 */
@RunWith(Parameterized.class)
public class SemuxApiErrorTest extends SemuxApiTestBase {

    private static final String ADDRESS_PLACEHOLDER = "[wallet]";

    @Parameters(name = "request(\"{1}\")")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { POST.class, uriBuilder("addNode").build() },

                { POST.class, uriBuilder("addNode").queryParam("node", "I_am_not_a_node").build() },

                { POST.class, uriBuilder("addNode").queryParam("node", "127.0.0.1:65536").build() },

                { POST.class, uriBuilder("addNode").queryParam("node", ".com:65536").build() },

                { PUT.class, uriBuilder("addToBlacklist").build() },

                { PUT.class, uriBuilder("addToBlacklist").queryParam("ip", "I_am_not_an_ip").build() },

                { PUT.class, uriBuilder("addToWhitelist").build() },

                { PUT.class, uriBuilder("addToWhitelist").queryParam("ip", "I_am_not_an_ip").build() },

                { GET.class, uriBuilder("getBlockByNumber").build() },

                { GET.class, uriBuilder("getBlockByNumber").queryParam("number", "9999999999999999").build() },

                { GET.class, uriBuilder("getBlockByHash").build() },

                { GET.class, uriBuilder("getBlockByHash").queryParam("hash", "xxx").build() },

                { GET.class, uriBuilder("getAccount").build() },

                { GET.class, uriBuilder("getAccount").queryParam("address", "0xabc").build() },

                { GET.class, uriBuilder("getAccount").queryParam("address", "I_am_not_an_address").build() },

                { GET.class, uriBuilder("getDelegate").build() },

                { GET.class, uriBuilder("getDelegate").queryParam("address", Hex.encode(Bytes.random(20))).build() },

                { GET.class, uriBuilder("getDelegate").queryParam("address", "I_am_not_an_address").build() },

                { GET.class, uriBuilder("getAccountTransactions").build() },

                { GET.class,
                        uriBuilder("getAccountTransactions").queryParam("address", "I_am_not_an_address").build() },

                { GET.class, uriBuilder("getAccountTransactions").queryParam("address", randomHex()).build() },

                { GET.class,
                        uriBuilder("getAccountTransactions").queryParam("address", randomHex())
                                .queryParam("from", "I_am_not_a_number").build() },

                { GET.class,
                        uriBuilder("getAccountTransactions").queryParam("address", randomHex()).queryParam("from", "0")
                                .queryParam("to", "I_am_not_a_number").build() },

                { GET.class, uriBuilder("getTransaction").build() },

                { GET.class, uriBuilder("getTransaction").queryParam("hash", "I_am_not_a_hexadecimal_string").build() },

                { GET.class, uriBuilder("getTransaction").queryParam("hash", randomHex()).build() },

                { POST.class, uriBuilder("broadcastRawTransaction").build() },

                { POST.class,
                        uriBuilder("broadcastRawTransaction").queryParam("raw", "I_am_not_a_hexadecimal_string")
                                .build() },

                { POST.class,
                        uriBuilder("broadcastRawTransaction").queryParam("raw", Hex.encode0x(RandomUtils.nextBytes(10)))
                                .build() },

                { GET.class, uriBuilder("getVote").build() },

                { GET.class, uriBuilder("getVote").queryParam("voter", "I_am_not_a_valid_address").build() },

                { GET.class, uriBuilder("getVote").queryParam("voter", randomHex()).build() },

                { GET.class,
                        uriBuilder("getVote").queryParam("voter", randomHex())
                                .queryParam("delegate", "I_am_not_a_valid_address").build() },

                { GET.class, uriBuilder("getVotes").build() },

                { GET.class, uriBuilder("getVotes").queryParam("delegate", "I_am_not_hexadecimal_string").build() },

                { POST.class, uriBuilder("transfer").build() },

                // non-hexadecimal address
                { POST.class, uriBuilder("transfer").queryParam("from", "_").build() },

                // non-wallet address
                { POST.class, uriBuilder("transfer").queryParam("from", randomHex()).build() },

                { POST.class, uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).build() },

                // non-hexadecimal to
                { POST.class,
                        uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).queryParam("to", "_").build() },

                { POST.class,
                        uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).queryParam("to", randomHex())
                                .build() },

                // non-number
                { POST.class,
                        uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).queryParam("to", randomHex())
                                .queryParam("value", "_").build() },

                // non-number
                { POST.class,
                        uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).queryParam("to", randomHex())
                                .queryParam("value", "10").queryParam("fee", "_").build() },

                { POST.class,
                        uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).queryParam("to", randomHex())
                                .queryParam("value", "10").queryParam("fee", "10").build() },

                // non-hexadecimal data
                { POST.class,
                        uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).queryParam("to", randomHex())
                                .queryParam("value", "10").queryParam("fee", "10").queryParam("data", "_").build() },

                { POST.class,
                        uriBuilder("transfer").queryParam("from", ADDRESS_PLACEHOLDER).queryParam("to", randomHex())
                                .queryParam("value", "10").queryParam("fee", "10").queryParam("data", randomHex())
                                .build() },

                { GET.class, uriBuilder("getTransactionLimits").build() },

                { GET.class, uriBuilder("getTransactionLimits").queryParam("type", "_").build() },
        });
    }

    private static UriBuilder uriBuilder(String methodName) {
        return UriBuilder.fromMethod(SemuxApi.class, methodName);
    }

    @Parameter(0)
    public Class<?> httpMethod;

    @Parameter(1)
    public URI uri;

    @Test
    public void testError() throws IOException {
        String uriString = uri.getPath() + (uri.getQuery() != null
                ? "?" + uri.getQuery().replace(ADDRESS_PLACEHOLDER, wallet.getAccount(0).toAddressString())
                : "");

        WebClient webClient = WebClient.create(
                String.format("http://%s:%d/%s%s", config.apiListenIp(), config.apiListenPort(),
                        ApiVersion.DEFAULT.prefix, uriString),
                Collections.singletonList(new JacksonJsonProvider(
                        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))),
                config.apiUsername(),
                config.apiPassword(),
                null);
        Response response = webClient.invoke(httpMethod.getSimpleName(), null);
        assertNotNull(response);

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