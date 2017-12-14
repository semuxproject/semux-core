/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.api.exception.ApiHandlerException;
import org.semux.api.response.ApiHandlerResponse;
import org.semux.util.BasicAuth;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.netty.handler.codec.http.HttpHeaders;

public class HttpHandlerTest {

    private static String ip = "127.0.0.1";
    private static int port = 15171;

    private String uri = null;
    private Map<String, String> params = null;
    private HttpHeaders headers = null;

    private KernelMock kernel;
    private SemuxAPI server;
    private String auth;

    @Before
    public void setup() {
        kernel = new KernelMock();
        server = new SemuxAPI(kernel);
        auth = BasicAuth.generateAuth(kernel.getConfig().apiUsername(), kernel.getConfig().apiPassword());
    }

    private void startServer(HttpChannelInitializer httpChannelInitializer) {
        // wait for server to boot up
        new Thread(() -> server.start(ip, port, httpChannelInitializer == null ? new HttpChannelInitializer() {
            @Override
            HttpHandler initHandler() {
                return new HttpHandler(kernel.getConfig(), (u, p, h) -> {
                    uri = u;
                    params = p;
                    headers = h;

                    return new ApiHandlerResponse(true, "test");
                });
            }
        } : httpChannelInitializer)).start();

        await().until(() -> server.isRunning());
    }

    @Test(expected = IOException.class)
    public void testAuth() throws IOException {
        startServer(null);

        URL url = new URL("http://" + ip + ":" + port + "/getinfo");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("c", "d");
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream().write("e=f".getBytes());

        Scanner s = new Scanner(con.getInputStream());
        s.nextLine();
        s.close();
    }

    @Test
    public void testPOST() throws IOException {
        startServer(null);

        URL url = new URL("http://" + ip + ":" + port + "/test?a=b");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("c", "d");
        con.setRequestProperty("Authorization", auth);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream().write("e=f".getBytes());

        Scanner s = new Scanner(con.getInputStream());
        s.nextLine();
        s.close();

        assertEquals("/test", uri);
        assertEquals("b", params.get("a"));
        assertEquals("f", params.get("e"));
        assertEquals("d", headers.get("c"));
    }

    @Test
    public void testGET() throws IOException {
        startServer(null);

        URL url = new URL("http://" + ip + ":" + port + "/test?a=b&e=f");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("c", "d");
        con.setRequestProperty("Authorization", auth);
        Scanner s = new Scanner(con.getInputStream());
        s.nextLine();
        s.close();

        assertEquals("/test", uri);
        assertEquals("b", params.get("a"));
        assertEquals("f", params.get("e"));
        assertEquals("d", headers.get("c"));
    }

    @Test
    public void testApiException() throws IOException {
        startServer(new HttpChannelInitializer() {
            @Override
            HttpHandler initHandler() {
                return new HttpHandler(kernel.getConfig(), (u, p, h) -> {
                    throw new ApiHandlerException("Internal Server Error", INTERNAL_SERVER_ERROR);
                });
            }
        });

        URL url = new URL("http://" + ip + ":" + port + "/test?a=b&e=f");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", auth);
        assertEquals(INTERNAL_SERVER_ERROR.code(), con.getResponseCode());
    }

    @Test
    public void testApiSerializationError() throws IOException {
        ApiHandlerResponse response = mock(ApiHandlerResponse.class);
        when(response.serialize()).thenThrow(JsonProcessingException.class);
        startServer(new HttpChannelInitializer() {
            @Override
            HttpHandler initHandler() {
                return new HttpHandler(kernel.getConfig(), (u, p, h) -> response);
            }
        });

        URL url = new URL("http://" + ip + ":" + port + "/test?a=b&e=f");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", auth);
        assertEquals(INTERNAL_SERVER_ERROR.code(), con.getResponseCode());
    }

    @After
    public void teardown() {
        server.stop();
    }
}
