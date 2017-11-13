package org.semux.api;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;

import org.junit.Test;
import org.semux.Config;
import org.semux.util.Bytes;

import io.netty.handler.codec.http.HttpHeaders;

public class SemuxHttpHandlerTest {

    private static String ip = "127.0.0.1";
    private static int port = 5171;

    private String uri = null;
    private Map<String, String> params = null;
    private HttpHeaders headers = null;

    private String auth = "Basic "
            + Base64.getEncoder().encodeToString(Bytes.of(Config.API_USERNAME + ":" + Config.API_PASSWORD));

    @Test(expected = IOException.class)
    public void testAuth() throws IOException {
        SemuxAPI server = new SemuxAPI(new ApiHandler() {
            @Override
            public String service(String u, Map<String, String> p, HttpHeaders h) {
                uri = u;
                params = p;
                headers = h;

                return "test";
            }
        });

        // wait for server to boot up
        new Thread(() -> {
            server.start(ip, port);
        }).start();
        while (!server.isListening()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }

        try {
            URL url = new URL("http://" + ip + ":" + port + "/getinfo");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("c", "d");
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.getOutputStream().write("e=f".getBytes());

            Scanner s = new Scanner(con.getInputStream());
            s.nextLine();
            s.close();
        } finally {
            server.stop();
        }
    }

    @Test
    public void testPOST() throws IOException {
        SemuxAPI server = new SemuxAPI(new ApiHandler() {
            @Override
            public String service(String u, Map<String, String> p, HttpHeaders h) {
                uri = u;
                params = p;
                headers = h;

                return "test";
            }
        });

        // wait for server to boot up
        new Thread(() -> {
            server.start(ip, port);
        }).start();
        while (!server.isListening()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }

        try {
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
        } finally {
            server.stop();
        }
    }

    @Test
    public void testGET() throws IOException {
        SemuxAPI server = new SemuxAPI(new ApiHandler() {
            @Override
            public String service(String u, Map<String, String> p, HttpHeaders h) {
                uri = u;
                params = p;
                headers = h;

                return "test";
            }
        });

        // wait for server to boot up
        new Thread(() -> {
            server.start(ip, port);
        }).start();
        while (!server.isListening()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }

        try {
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
        } finally {
            server.stop();
        }
    }
}
