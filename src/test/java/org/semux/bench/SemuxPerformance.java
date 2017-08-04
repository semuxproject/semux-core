package org.semux.bench;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;
import org.semux.Config;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.core.Unit;
import org.semux.core.Wallet;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hex;
import org.semux.utils.Bytes;
import org.semux.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxPerformance {
    private static Logger logger = LoggerFactory.getLogger(SemuxPerformance.class);

    private static Wallet wallet = Wallet.getInstance();

    private static JSONObject request(String uri) throws IOException {
        StringBuilder sb = new StringBuilder();
        URL url = new URL("http://localhost:5171" + uri);

        Scanner s = new Scanner(url.openStream());
        while (s.hasNextLine()) {
            sb.append(s.nextLine());
        }
        s.close();

        return new JSONObject(sb.toString());
    }

    public static void testTransfer(EdDSA key, int n) throws IOException {

        JSONObject obj = request("/get_nonce?address=" + key.toAddressString());
        long startingNonce = obj.getLong("result") + 1;

        for (int i = 0; i < n; i++) {
            TransactionType type = TransactionType.TRANSFER;
            byte[] from = key.toAddress();
            byte[] to = Bytes.random(20);
            long value = 1;
            long fee = Config.MIN_TRANSACTION_FEE;
            long nonce = startingNonce + i;
            long timestamp = System.currentTimeMillis();
            byte[] data = {};
            Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
            tx.sign(key);

            request("/send_transaction?raw=" + Hex.encode(tx.toBytes()));
        }
    }

    public static void main(String[] args) throws IOException {
        wallet.unlock(SystemUtil.readPassword());

        if (wallet.getAccounts().isEmpty()) {
            logger.error("No accounts in your wallet");
            return;
        }

        EdDSA key = wallet.getAccounts().get(0);
        JSONObject obj = request("/get_balance?address=" + key.toAddressString());
        System.out.println("Address: " + key.toAddressString());
        System.out.println("Balance: " + obj.getLong("result") / Unit.SEM + " SEM");

        while (true) {
            System.out.print("# txs to send: ");
            System.out.flush();

            int n = Integer.parseInt(SystemUtil.SCANNER.nextLine());
            if (n > 0) {
                testTransfer(key, n);
            } else {
                break;
            }
        }
    }
}
