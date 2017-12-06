/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.semux.config.Constants;
import org.semux.crypto.Hex;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Genesis extends Block {

    private static final Logger logger = LoggerFactory.getLogger(Genesis.class);

    private static final String GENESIS_FILE = "genesis.json";

    private Map<ByteArray, Premine> premines;
    private Map<String, byte[]> delegates;
    private JsonObject config;

    /**
     * Creates a {@link Genesis} block instance.
     * 
     * @param header
     * @param premine
     * @param delegates
     * @param config
     */
    public Genesis(BlockHeader header, Map<ByteArray, Premine> premine, Map<String, byte[]> delegates,
            JsonObject config) {
        super(header, Collections.emptyList(), Collections.emptyList());

        this.premines = premine;
        this.delegates = delegates;
        this.config = config;
    }

    /**
     * Loads the genesis file.
     * 
     * @return
     */
    public static Genesis load(File dataDir) {
        Path path = Paths.get(dataDir.getAbsolutePath(), Constants.CONFIG_DIR, GENESIS_FILE);
        try (JsonReader jsonReader = Json.createReader(Files.newBufferedReader(path))) {
            JsonObject json = jsonReader.readObject();

            // block information
            long number = json.getJsonNumber("number").longValueExact();
            byte[] coinbase = Hex.parse(json.getString("coinbase"));
            byte[] prevHash = Hex.parse(json.getString("parentHash"));
            long timestamp = json.getJsonNumber("timestamp").longValueExact();
            byte[] data = Bytes.of(json.getString("data"));

            // premine
            Map<ByteArray, Premine> premine = new HashMap<>();
            JsonArray arr = json.getJsonArray("premine");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.getJsonObject(i);
                byte[] address = Hex.parse(obj.getString("address"));
                long amount = obj.getJsonNumber("amount").longValueExact() * Unit.SEM;
                premine.put(ByteArray.of(address), new Premine(address, amount));
            }

            // delegates
            Map<String, byte[]> delegates = new HashMap<>();
            JsonObject obj = json.getJsonObject("delegates");
            for (String k : obj.keySet()) {
                byte[] address = Hex.parse(obj.getString(k));
                delegates.put(k, address);
            }

            // configurations
            JsonObject config = json.getJsonObject("config");

            BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, Bytes.EMPTY_HASH,
                    Bytes.EMPTY_HASH, Bytes.EMPTY_HASH, data);
            return new Genesis(header, premine, delegates, config);
        } catch (IOException e) {
            logger.error("Failed to load genesis file", e);
            SystemUtil.exitAsync(-1);
            return null;
        }
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
    public JsonObject getConfig() {
        return config;
    }

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
}