package org.semux.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONObject;

public class ApiUtil {
    private static InetSocketAddress DEFAULT_SERVER = new InetSocketAddress("127.0.0.1", 5171);

    public static JSONObject request(InetSocketAddress server, String cmd, Map<String, Object> params)
            throws IOException {
        String url = "http://" + server.getAddress().getHostAddress() + ":" + server.getPort() + "/" + cmd;

        StringBuilder sb = new StringBuilder();
        for (String k : params.keySet()) {
            sb.append("&" + k + "=" + URLEncoder.encode(params.get(k).toString(), "UTF-8"));
        }
        url += sb.length() == 0 ? "" : "?" + sb.substring(1);
        sb.setLength(0);

        try (Scanner s = new Scanner(new URL(url).openStream())) {
            while (s.hasNextLine()) {
                sb.append(s.nextLine());
            }
        }

        return new JSONObject(sb.toString());
    }

    public static JSONObject request(String cmd, Map<String, Object> params) throws IOException {
        return request(DEFAULT_SERVER, cmd, params);
    }
}
