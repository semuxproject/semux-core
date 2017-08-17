package org.semux.bench;

import java.io.IOException;
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

    private static Wallet wallet = Wallet.getInstance();

    public static long getNonce(EdDSA key) throws IOException {
        String cmd = "get_nonce";

        JSONObject response = ApiUtil.request(cmd, "address", key.toAddress());
        if (response.getBoolean("success")) {
            return response.getLong("result");
        } else {
            throw new IOException(response.toString());
        }
    }

    public static void testTransfer(EdDSA key, int n) throws IOException {
        long startNonce = getNonce(key) + 1;

        String cmd = "transfer";

        for (int i = 0; i < n; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("from", key.toAddress());
            params.put("to", Bytes.random(20));
            params.put("value", 1 * Unit.MILLI_SEM);
            params.put("fee", Config.MIN_TRANSACTION_FEE);
            params.put("nonce", startNonce + i);
            params.put("data", Bytes.EMPY_BYTES);

            JSONObject response = ApiUtil.request(cmd, params);
            if (!response.getBoolean("success")) {
                System.out.println(response);
                return;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (!wallet.unlock(SystemUtil.readPassword())) {
            logger.error("Failed to unlock your wallet");
            return;
        }

        if (wallet.getAccounts().isEmpty()) {
            logger.error("No accounts in your wallet");
            return;
        }

        EdDSA key = wallet.getAccounts().get(0);
        JSONObject obj = ApiUtil.request("get_balance", "address", key.toAddressString());
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
