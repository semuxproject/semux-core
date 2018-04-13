/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.http;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.api.Version;
import org.semux.api.v1_0_1.ApiHandlerResponse;
import org.semux.rules.KernelRule;
import org.semux.util.BasicAuth;

import io.netty.handler.codec.http.HttpHeaders;

@SuppressWarnings("deprecation")
public class HttpHandlerTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    private String uri = null;
    private Map<String, String> params = null;
    private HttpHeaders headers = null;

    private KernelMock kernel;
    private SemuxApiService server;

    private String ip;
    private int port;
    private String auth;

    @Before
    public void setUp() {
        kernel = kernelRule.getKernel();
        server = new SemuxApiService(kernel);

        ip = kernel.getConfig().apiListenIp();
        port = kernel.getConfig().apiListenPort();
        auth = BasicAuth.generateAuth(kernel.getConfig().apiUsername(), kernel.getConfig().apiPassword());
    }

    @After
    public void tearDown() {
        server.stop();
    }

    private void startServer(HttpChannelInitializer httpChannelInitializer) {
        // wait for server to boot up
        new Thread(() -> server.start(ip, port, httpChannelInitializer == null ? new HttpChannelInitializer() {
            @Override
            HttpHandler initHandler() {
                return new HttpHandler(kernel.getConfig(), (m, u, p, h) -> {
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
    public void testGetStaticFiles() throws IOException {
        startServer(null);

        URL url = new URL(server.getSwaggerUrl());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", auth);

        StringBuilder lines = new StringBuilder();
        Scanner s = new Scanner(con.getInputStream());
        while (s.hasNextLine()) {
            lines.append(s.nextLine());
        }
        s.close();

        assertEquals(HTTP_OK, con.getResponseCode());
        assertEquals("text/html", con.getHeaderField("content-type"));
        assertTrue(lines.toString().length() > 1);
    }

    @Test
    public void testGetStaticFiles404() throws IOException {
        startServer(null);

        URL url = new URL("http://" + ip + ":" + port + "/" + Version.prefixOf(Version.v2_0_0) + "/xx.html");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", auth);

        assertEquals(HTTP_NOT_FOUND, con.getResponseCode());
    }

    @Test
    public void testKeepAlive() throws IOException {
        startServer(null);

        for (int i = 0; i < 2; i++) {
            URL url = new URL("http://" + ip + ":" + port + "/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("Authorization", auth);

            Scanner s = new Scanner(con.getInputStream());
            s.nextLine();
            s.close();

            assertEquals(HTTP_OK, con.getResponseCode());
            assertEquals("keep-alive", con.getHeaderField("connection"));
        }
    }
}
