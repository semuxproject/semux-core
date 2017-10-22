package org.semux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;
import org.semux.utils.ApiUtil;
import org.semux.utils.Bytes;

public class Testnet {
    private static int from = 4;

    public static void transfer(byte[] to, long value) throws IOException {
        String cmd = "transfer";

        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("to", to);
        params.put("value", value);
        params.put("fee", Config.MIN_TRANSACTION_FEE_HARD);
        params.put("data", Bytes.EMPY_BYTES);

        JSONObject response = ApiUtil.request(cmd, params);
        System.out.println(response);
    }

    public static void registerDelegate(byte[] address, byte[] name) throws IOException {
        String cmd = "delegate";

        Map<String, Object> params = new HashMap<>();
        params.put("from", address);
        params.put("to", address);
        params.put("value", Config.DELEGATE_BURN_AMOUNT);
        params.put("fee", Config.MIN_TRANSACTION_FEE_HARD);
        params.put("data", name);

        JSONObject response = ApiUtil.request(cmd, params);
        System.out.println(response);
    }

    public static void main(String[] args) throws IOException {
        // transfer(Hex.decode("3b0d9d0cd8fc121eae10ab033b6816b1b0ddb6a6"), 5000 *
        // Unit.SEM);
        // transfer(Hex.decode("5ddf7affa7527a0dc642ca5e126dbb4352596e84"), 5000 *
        // Unit.SEM);
        // transfer(Hex.decode("2b1862ccb93abdc2c6a45282818f1c2f4ba8605b"), 5000 *
        // Unit.SEM);
        // transfer(Hex.decode("4a86f25888685e50d34e3828819c347d3e4b686a"), 5000 *
        // Unit.SEM);

        registerDelegate(Hex.decode("5ddf7affa7527a0dc642ca5e126dbb4352596e84"), Bytes.of("node6"));
    }
}
