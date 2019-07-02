/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.semux.api.ApiVersion;
import org.semux.config.Constants;
import org.semux.crypto.Hex;

/**
 * A simple implementation of Semux API client. It's designed to be schemaless
 * and should be used for testing purpose only.
 */
public class SimpleApiClient {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = Constants.DEFAULT_API_PORT;
    public static final String DEFAULT_VERSION = ApiVersion.DEFAULT.prefix;

    private final String root;
    private final String username;
    private final String password;

    /**
     * Crates an API client instance.
     *
     * @param root
     *            the API service root
     * @param username
     *            the username
     * @param password
     *            the password
     */
    public SimpleApiClient(String root, String username, String password) {
        this.root = root;
        this.username = username;
        this.password = password;
    }

    public SimpleApiClient(String ip, int port, String version, String username, String password) {
        this("http://" + ip + ":" + port + "/" + version, username, password);
    }

    public SimpleApiClient(String ip, int port, String username, String password) {
        this(ip, port, DEFAULT_VERSION, username, password);
    }

    public SimpleApiClient(String username, String password) {
        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_VERSION, username, password);
    }

    /**
     * Sends a request to the API server. This method automatically handles
     * parameter types:
     * <p>
     * <ul>
     * <li>Basic type values will be converted into its string representation</li>
     * <li>Byte arrays will be converted into its hex representation</li>
     * <li>Objects will be converted into its <code>toString()</code> response</li>
     * </ul>
     *
     * @param method
     *            the request method, "GET", "POST", "PUT" or "DELETE"
     * @param uri
     *            the resource URI, starting with slash
     * @param params
     *            the input parameters
     * @return
     * @throws IOException
     */
    protected String request(String method, String uri, Map<String, Object> params) throws IOException {
        // construct URL
        String url = root + uri;

        // construct parameters
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String v;
            if (entry.getValue() instanceof byte[]) {
                v = Hex.encode((byte[]) entry.getValue());
            } else {
                v = URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.name());
            }
            sb.append("&").append(entry.getKey()).append("=").append(v);
        }
        if (sb.length() != 0 && "GET".equalsIgnoreCase(method)) {
            url += "?" + sb.substring(1);
        }

        // request
        URL u = new URL(url);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        con.setRequestProperty("Authorization", BasicAuth.generateAuth(username, password));
        con.setRequestMethod(method);

        if (sb.length() != 0 && !"GET".equalsIgnoreCase(method)) {
            con.setDoOutput(true);
            con.getOutputStream().write(Bytes.of(sb.substring(1)));
        }

        return IOUtil.readStreamAsString(con.getInputStream());
    }

    protected String request(String method, String uri, Object... params) throws IOException {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < params.length; i += 2) {
            map.put(params[i].toString(), params[i + 1]);
        }

        return request(method, uri, map);
    }

    public String get(String uri, Map<String, Object> params) throws IOException {
        return request("GET", uri, params);
    }

    public String get(String uri, Object... params) throws IOException {
        return request("GET", uri, params);
    }

    public String post(String uri, Map<String, Object> params) throws IOException {
        return request("POST", uri, params);
    }

    public String post(String uri, Object... params) throws IOException {
        return request("POST", uri, params);
    }

    public String put(String uri, Map<String, Object> params) throws IOException {
        return request("PUT", uri, params);
    }

    public String put(String uri, Object... params) throws IOException {
        return request("PUT", uri, params);
    }

    public String delete(String uri, Map<String, Object> params) throws IOException {
        return request("DELETE", uri, params);
    }

    public String delete(String uri, Object... params) throws IOException {
        return request("DELETE", uri, params);
    }
}
