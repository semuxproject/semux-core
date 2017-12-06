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
import java.util.List;
import java.util.Map;

import org.semux.config.Constants;
import org.semux.crypto.Hex;
import org.semux.util.ByteArray;
import org.semux.util.ByteArray.ByteArrayKeyDeserializer;
import org.semux.util.Bytes;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Genesis extends Block {

    private static final Logger logger = LoggerFactory.getLogger(Genesis.class);

    private static final String GENESIS_FILE = "genesis.json";

    @JsonDeserialize(keyUsing = ByteArrayKeyDeserializer.class)
    private Map<ByteArray, Premine> premines;

    private Map<String, byte[]> delegates;

    private Map<String, Object> config;

    /**
     * Creates a {@link Genesis} block instance.
     * 
     * @param header
     * @param premine
     * @param delegates
     * @param config
     */
    public Genesis(BlockHeader header, Map<ByteArray, Premine> premine, Map<String, byte[]> delegates,
            Map<String, Object> config) {
        super(header, Collections.emptyList(), Collections.emptyList());

        this.premines = premine;
        this.delegates = delegates;
        this.config = config;
    }

    @JsonCreator
    public static Genesis jsonCreator(
            @JsonProperty("number") long number,
            @JsonProperty("coinbase") String coinbase,
            @JsonProperty("parentHash") String parentHash,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("data") String data,
            @JsonProperty("premine") List<Premine> premineList,
            @JsonProperty("delegates") Map<String, String> delegates,
            @JsonProperty("config") Map<String, Object> config) {
        // load premines
        Map<ByteArray, Premine> premineMap = new HashMap<>();
        for (Premine premine : premineList) {
            premineMap.put(new ByteArray(premine.getAddress()), premine);
        }

        // load block header
        BlockHeader header = new BlockHeader(
                number,
                Hex.parse(coinbase),
                Hex.parse(parentHash),
                timestamp,
                Bytes.EMPTY_HASH,
                Bytes.EMPTY_HASH,
                Bytes.EMPTY_HASH,
                Bytes.of(data));

        // load initial delegates
        Map<String, byte[]> delegatesMap = new HashMap<>();
        for (Map.Entry<String, String> entry : delegates.entrySet()) {
            delegatesMap.put(entry.getKey(), Hex.parse(entry.getValue()));
        }

        return new Genesis(header, premineMap, delegatesMap, config);
    }

    /**
     * Loads the genesis file.
     * 
     * @return
     */
    public static Genesis load(File dataDir) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    Paths.get(dataDir.getAbsolutePath(), Constants.CONFIG_DIR, GENESIS_FILE).toFile(),
                    Genesis.class);
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
    public Map<String, Object> getConfig() {
        return config;
    }

    public static class Premine {
        private byte[] address;
        private long amount;
        private String note;

        public Premine(byte[] address, long amount, String note) {
            this.address = address;
            this.amount = amount;
            this.note = note;
        }

        @JsonCreator
        public Premine(
                @JsonProperty("address") String address,
                @JsonProperty("amount") long amount,
                @JsonProperty("note") String note) {
            this(Bytes.of(address), amount, note);
        }

        public byte[] getAddress() {
            return address;
        }

        public long getAmount() {
            return amount;
        }

        public String getNote() {
            return note;
        }
    }
}
