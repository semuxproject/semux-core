/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.semux.Config;
import org.semux.crypto.Hex;
import org.semux.utils.ByteArray;
import org.semux.utils.Bytes;
import org.semux.utils.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Genesis extends Block {

    private static final Logger logger = LoggerFactory.getLogger(Genesis.class);

    private static final String GENESIS_DIR = "config";
    private static final String GENESIS_FILE = "genesis.json";

    private Map<ByteArray, Long> premine;
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
                String str = IOUtil
                        .readFileAsString(new File(Config.DATA_DIR, GENESIS_DIR + File.separator + GENESIS_FILE));
                JSONObject json = new JSONObject(str);

                // block information
                long number = json.getLong("number");
                byte[] coinbase = Hex.parse(json.getString("coinbase"));
                byte[] prevHash = Hex.parse(json.getString("parentHash"));
                long timestamp = json.getLong("timestamp");
                byte[] merkelRoot = Hex.parse(json.getString("merkleRoot"));
                byte[] data = Bytes.of(json.getString("data"));
                List<Transaction> transactions = new ArrayList<>();

                // premine
                Map<ByteArray, Long> premine = new HashMap<>();
                JSONObject obj = json.getJSONObject("premine");
                for (String k : obj.keySet()) {
                    byte[] addr = Hex.parse(k);
                    long balance = obj.getJSONObject(k).getLong("balance") * Unit.SEM;
                    premine.put(ByteArray.of(addr), balance);
                }

                // delegates
                Map<String, byte[]> delegates = new HashMap<>();
                obj = json.getJSONObject("delegates");
                for (String k : obj.keySet()) {
                    byte[] address = Hex.parse(obj.getString(k));
                    delegates.put(k, address);
                }

                // configurations
                Map<String, Object> config = json.getJSONObject("config").toMap();

                instance = new Genesis(number, coinbase, prevHash, timestamp, merkelRoot, data, transactions, premine,
                        delegates, config);
            } catch (IOException e) {
                logger.error("Failed to load genesis file", e);
                System.exit(-1);
            }
        }

        return instance;
    }

    private Genesis(long number, byte[] coinbase, byte[] prevHash, long timestamp, byte[] merkleRoot, byte[] data,
            List<Transaction> transactions, //
            Map<ByteArray, Long> alloc, Map<String, byte[]> delegates, Map<String, Object> config) {
        super(number, coinbase, prevHash, timestamp, merkleRoot, data, transactions);

        this.premine = alloc;
        this.delegates = delegates;
        this.config = config;

        this.isGensis = true;
    }

    /**
     * Get premine.
     * 
     * @return
     */
    public Map<ByteArray, Long> getPremine() {
        return premine;
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
