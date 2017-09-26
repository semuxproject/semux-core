package org.semux.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONObject;
import org.semux.crypto.Hex;

public class ApiUtil {
    private static InetSocketAddress DEFAULT_SERVER = new InetSocketAddress("127.0.0.1", 5171);

    /**
     * Send an API request. <br>
     * <br>
     * NOTE: Byte array parameters will be automatically converted into its Hex
     * representation.
     * 
     * @param server
     * @param cmd
     * @param params
     * @return
     * @throws IOException
     */
    public static JSONObject request(InetSocketAddress server, String cmd, Map<String, Object> params)
            throws IOException {
        // construct URL
        String url = "http://" + server.getAddress().getHostAddress() + ":" + server.getPort() + "/" + cmd;

        // construct parameters
        StringBuilder sb = new StringBuilder();
        for (String k : params.keySet()) {
            String v = null;
            if (params.get(k) instanceof byte[]) {
                v = Hex.encode((byte[]) params.get(k));
            } else {
                v = URLEncoder.encode(params.get(k).toString(), "UTF-8");
            }
            sb.append("&" + k + "=" + v);
        }
        url += sb.length() == 0 ? "" : "?" + sb.substring(1);
        sb.setLength(0);

        // download
        try (Scanner s = new Scanner(new URL(url).openStream())) {
            while (s.hasNextLine()) {
                sb.append(s.nextLine());
            }
        }

        return new JSONObject(sb.toString());
    }

    /**
     * Send an API request.
     * 
     * @param cmd
     * @param params
     * @return
     * @throws IOException
     */
    public static JSONObject request(String cmd, Map<String, Object> params) throws IOException {
        return request(DEFAULT_SERVER, cmd, params);
    }

    /**
     * Send an API request.
     * 
     * @param server
     * @param cmd
     * @param params
     * @return
     * @throws IOException
     */
    public static JSONObject request(InetSocketAddress server, String cmd, Object... params) throws IOException {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < params.length; i += 2) {
            map.put(params[i].toString(), params[i + 1]);
        }

        return request(server, cmd, map);
    }

    /**
     * Send an API request.
     * 
     * @param cmd
     * @param params
     * @return
     * @throws IOException
     */
    public static JSONObject request(String cmd, Object... params) throws IOException {
        return request(DEFAULT_SERVER, cmd, params);
    }
}
