/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import org.semux.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class SemuxIpFilter {

    private static final Logger logger = LoggerFactory.getLogger(SemuxIpFilter.class);

    /**
     * CopyOnWriteArrayList allows APIs to update rules atomically without affecting
     * the performance of read-only iteration
     */
    private CopyOnWriteArrayList<IpFilterRule> rules;

    public SemuxIpFilter(Collection<IpFilterRule> rules) {
        this.rules = new CopyOnWriteArrayList<>(rules);
    }

    public SemuxIpFilter() {
        this.rules = new CopyOnWriteArrayList<>();
    }

    /**
     * isBlocked method matches supplied address against defined rules sequentially
     * and returns a result based on the first matched rule's type
     * 
     * @param address
     *            an address which will be matched against defined rules
     * @return whether the address is blocked or not
     */
    public boolean isBlocked(InetSocketAddress address) {
        return rules.stream().filter(rule -> rule != null && rule.matches(address)).findFirst().flatMap(rule -> {
            if (rule.ruleType() == IpFilterRuleType.REJECT) {
                return Optional.of(true);
            } else {
                return Optional.of(false);
            }
        }).orElse(false);
    }

    /**
     * Block a single IP at runtime
     *
     * @param ip
     * @throws UnknownHostException
     */
    public void blockIp(String ip) throws UnknownHostException {
        // prepend a REJECT IP rule to the rules list to ensure that the IP will be
        // blocked
        rules.add(0, new IpSingleFilterRule(ip, IpFilterRuleType.REJECT));
        logger.info("Blocked IP {}", ip);
    }

    public static final class Builder {

        private ArrayList<IpFilterRule> rules = new ArrayList<>();

        private void addRule(String cidrNotation, IpFilterRuleType type) throws UnknownHostException {
            IpFilterRule ipSubnetFilterRule = new CIDRFilterRule(cidrNotation, type);
            rules.add(ipSubnetFilterRule);
        }

        public Builder accept(String cidrNotation) throws UnknownHostException {
            addRule(cidrNotation, IpFilterRuleType.ACCEPT);
            return this;
        }

        public Builder reject(String cidrNotation) throws UnknownHostException {
            addRule(cidrNotation, IpFilterRuleType.REJECT);
            return this;
        }

        public SemuxIpFilter build() {
            return new SemuxIpFilter(rules);
        }
    }

    public static final class Loader {

        private static final Logger logger = LoggerFactory.getLogger(Loader.class);

        private static final String IPFILTER_JSON = "ipfilter.json";

        public Optional<SemuxIpFilter> load() {
            return loadJson().flatMap(this::parseJson);
        }

        private Optional<JsonObject> loadJson() {
            Path ipFilterJsonPath = Paths.get(Config.CONFIG_DIR, IPFILTER_JSON);
            if (!ipFilterJsonPath.toFile().exists()) {
                logger.info("{} doesn't exist, skip loading");
                return Optional.empty();
            }

            try (JsonReader jsonReader = Json.createReader(Files.newBufferedReader(ipFilterJsonPath))) {
                return Optional.of(jsonReader.readObject());
            } catch (IOException ex) {
                logger.error(String.format("Failed to load %s", ipFilterJsonPath.toAbsolutePath()), ex);
                return Optional.empty();
            }
        }

        private Optional<SemuxIpFilter> parseJson(JsonObject ipFilterJson) {
            Builder builder = new Builder();
            ipFilterJson.getJsonArray("rules").stream().map(JsonValue::asJsonObject).forEach(rule -> {
                IpFilterRuleType type = IpFilterRuleType.valueOf(rule.getString("type"));
                String address = rule.getString("address");
                try {
                    if (type == IpFilterRuleType.ACCEPT) {
                        logger.info("Loaded rule: ACCEPT {}", address);
                        builder.accept(address);
                    } else if (type == IpFilterRuleType.REJECT) {
                        logger.info("Loaded rule: REJECT {}", address);
                        builder.reject(address);
                    } else {
                        throw new IllegalArgumentException("Rule type of ip filter must be either ACCEPT or REJECT");
                    }
                } catch (UnknownHostException ex) {
                    throw new IllegalArgumentException(String.format("Invalid address %s", address), ex);
                }
            });
            return Optional.of(builder.build());
        }
    }
}
