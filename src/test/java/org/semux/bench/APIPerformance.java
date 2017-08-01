package org.semux.bench;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import org.semux.Config;
import org.semux.api.APIServerMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIPerformance {
    private static Logger logger = LoggerFactory.getLogger(APIPerformance.class);

    private static int REPEAT = 1000;

    public static void testBasic() throws IOException {
        APIServerMock api = new APIServerMock();
        api.start(Config.API_LISTEN_IP, Config.API_LISTEN_PORT);

        try {
            long t1 = System.nanoTime();
            for (int i = 0; i < REPEAT; i++) {
                URL url = new URL("http://localhost:" + Config.API_LISTEN_PORT);
                Scanner s = new Scanner(url.openStream());
                while (s.hasNextLine()) {
                    s.nextLine();
                }
                s.close();
            }
            long t2 = System.nanoTime();
            logger.info("Perf_api_basic: " + (t2 - t1) / 1_000 / REPEAT + " μs/time");
        } finally {
            api.stop();
        }
    }

    public static void main(String[] args) throws Exception {
        testBasic();
    }
}
