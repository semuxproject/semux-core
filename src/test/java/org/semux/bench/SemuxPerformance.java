package org.semux.bench;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.semux.Config;
import org.semux.core.Unit;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.utils.ApiUtil;
import org.semux.utils.Bytes;
import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxPerformance {
    private static Logger logger = LoggerFactory.getLogger(SemuxPerformance.class);

    private static Wallet wallet = new Wallet(new File("wallet.data"));
    private static String password;

    private static InetSocketAddress server = new InetSocketAddress("127.0.0.1", 5170);
    private static int coinbase = 1;
    private static int tps = 500;

    public static void testTransfer(EdDSA key, int n) throws IOException, InterruptedException {
        long t1 = System.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("from", key.toAddress());
            params.put("to", Bytes.random(20));
            params.put("value", 1 * Unit.MILLI_SEM);
            params.put("fee", Config.MIN_TRANSACTION_FEE);
            params.put("data", Bytes.EMPY_BYTES);
            params.put("password", password);

            JSONObject response = ApiUtil.request(server, "transfer", params);
            if (!response.getBoolean("success")) {
                System.out.println(response);
                return;
            }

            if (i % tps == 0) {
                System.out.println(new SimpleDateFormat("[HH:mm:ss]").format(new Date()) + " " + i);
                long t2 = System.currentTimeMillis();
                Thread.sleep(Math.max(0, 1000 - (t2 - t1)));
                t1 = t2;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        password = SystemUtil.readPassword();

        if (!wallet.unlock(password)) {
            logger.error("Failed to unlock your wallet");
            return;
        }

        if (wallet.getAccounts().isEmpty()) {
            logger.error("No accounts in your wallet");
            return;
        }

        EdDSA key = wallet.getAccounts().get(coinbase);
        JSONObject obj = ApiUtil.request(server, "get_balance", "address", key.toAddressString());
        System.out.println("Address: " + key.toAddressString());
        System.out.println("Balance: " + obj.getLong("result") / Unit.SEM + " SEM");

        while (true) {
            System.out.print("# transactions to send: ");
            System.out.flush();

            int n = Integer.parseInt(SystemUtil.SCANNER.nextLine().replaceAll("[^\\d]", ""));
            if (n > 0) {
                testTransfer(key, n);
            } else {
                break;
            }
        }
    }
}
