/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semux.Config;
import org.semux.crypto.Hex;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.IOUtil;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Genesis extends Block {

    public static class Premine {
        private byte[] address;
        private long amount;

        public Premine(byte[] address, long amount) {
            super();
            this.address = address;
            this.amount = amount;
        }

        public byte[] getAddress() {
            return address;
        }

        public long getAmount() {
            return amount;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Genesis.class);

    private static final String GENESIS_FILE = "genesis.json";

    private Map<ByteArray, Premine> premines;
    private Map<String, byte[]> delegates;
    private Map<String, Object> config;

    private static Genesis instance = null;

    /**
     * Get the singleton instance of the genesis block.
     * 
     * @return
     */
    public static synchronized Genesis getInstance() {
        if (instance == null) {
            try {
                File file = Paths.get(Config.DATA_DIR, Config.CONFIG_DIR, GENESIS_FILE).toFile();
                String str = IOUtil.readFileAsString(file);
                JSONObject json = new JSONObject(str);

                // block information
                long number = json.getLong("number");
                byte[] coinbase = Hex.parse(json.getString("coinbase"));
                byte[] prevHash = Hex.parse(json.getString("parentHash"));
                long timestamp = json.getLong("timestamp");
                byte[] data = Bytes.of(json.getString("data"));

                // premine
                Map<ByteArray, Premine> premine = new HashMap<>();
                JSONArray arr = json.getJSONArray("premine");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    byte[] address = Hex.parse(obj.getString("address"));
                    long amount = obj.getLong("amount") * Unit.SEM;
                    premine.put(ByteArray.of(address), new Premine(address, amount));
                }

                // delegates
                Map<String, byte[]> delegates = new HashMap<>();
                JSONObject obj = json.getJSONObject("delegates");
                for (String k : obj.keySet()) {
                    byte[] address = Hex.parse(obj.getString(k));
                    delegates.put(k, address);
                }

                // configurations
                Map<String, Object> config = json.getJSONObject("config").toMap();

                instance = new Genesis(number, coinbase, prevHash, timestamp, data, premine, delegates, config);
            } catch (IOException e) {
                logger.error("Failed to load genesis file", e);
                SystemUtil.exitAsync(-1);
            }
        }

        return instance;
    }

    private Genesis(long number, byte[] coinbase, byte[] prevHash, long timestamp, byte[] data,
            Map<ByteArray, Premine> premine, //
            Map<String, byte[]> delegates, //
            Map<String, Object> config) {
        super(new BlockHeader(number, coinbase, prevHash, timestamp, Bytes.EMPTY_HASH, Bytes.EMPTY_HASH,
                Bytes.EMPTY_HASH, data), Collections.emptyList(), Collections.emptyList());

        this.premines = premine;
        this.delegates = delegates;
        this.config = config;
    }

    /**
     * Get premine.
     * 
     * @return
     */
    public Map<ByteArray, Premine> getPremines() {
        return premines;
    }

    /**
     * Get delegates.
     * 
     * @return
     */
    public Map<String, byte[]> getDelegates() {
        return delegates;
    }

    /**
     * Get genesis configurations.
     * 
     * @return
     */
    public Map<String, Object> getConfig() {
        return config;
    }
}
