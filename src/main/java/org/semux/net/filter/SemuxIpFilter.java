/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import org.semux.net.filter.exception.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;

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

    public List<IpFilterRule> getRules() {
        return rules;
    }

    /**
     * isAcceptable method matches supplied address against defined rules
     * sequentially and returns a result based on the first matched rule's type
     * 
     * @param address
     *            an address which will be matched against defined rules
     * @return whether the address is blocked or not
     */
    public boolean isAcceptable(InetSocketAddress address) {
        return rules.stream().filter(rule -> rule != null && rule.matches(address)).findFirst().flatMap(rule -> {
            if (rule.ruleType() == IpFilterRuleType.ACCEPT) {
                return Optional.of(true);
            } else {
                return Optional.of(false);
            }
        }).orElse(true);
    }

    /**
     * Block a single IP at runtime
     *
     * @param ip
     * @throws UnknownHostException
     */
    public void blacklistIp(String ip) throws UnknownHostException {
        // prepend a REJECT IP rule to the rules list to ensure that the IP will be
        // blocked
        IpSingleFilterRule rule = new IpSingleFilterRule(ip, IpFilterRuleType.REJECT);
        rules.remove(rule);
        rules.add(0, rule);
        logger.info("Blacklisted IP {}", ip);
    }

    /**
     * Whitelist a single IP at runtime
     *
     * @param ip
     * @throws UnknownHostException
     */
    public void whitelistIp(String ip) throws UnknownHostException {
        // prepend an ACCEPT IP rule to the rules list to ensure that the IP will be
        // accepted
        IpSingleFilterRule rule = new IpSingleFilterRule(ip, IpFilterRuleType.ACCEPT);
        rules.remove(rule);
        rules.add(0, rule);
        logger.info("Whitelisted IP {}", ip);
    }

    public void appendRule(IpFilterRule rule) {
        rules.add(rule);
    }

    public void purgeRules() {
        rules.clear();
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

        public boolean addAll(Builder builder) {
            return this.rules.addAll(builder.getRules());
        }

        public List<IpFilterRule> getRules() {
            return rules;
        }

        public SemuxIpFilter build() {
            return new SemuxIpFilter(rules);
        }
    }

    public static final class Loader {

        private static final Logger logger = LoggerFactory.getLogger(Loader.class);

        public Optional<SemuxIpFilter> load(Path ipFilterJsonPath) {
            if (!ipFilterJsonPath.toFile().exists()) {
                logger.info("{} doesn't exist, skip loading");
                return Optional.empty();
            }

            return loadRules(ipFilterJsonPath).flatMap(this::parseRules);
        }

        private Optional<JsonArray> loadRules(Path ipFilterJsonPath) {
            try (JsonReader jsonReader = Json.createReader(Files.newBufferedReader(ipFilterJsonPath))) {
                JsonObject json = jsonReader.readObject();
                JsonArray rules = json.getJsonArray("rules");
                if (rules == null) {
                    throw new ParseException("rules field doesn't exist");
                }
                return Optional.of(rules);
            } catch (IOException ex) {
                logger.error(String.format("Failed to load %s", ipFilterJsonPath.toAbsolutePath()), ex);
                return Optional.empty();
            } catch (ClassCastException | JsonParsingException ex) {
                throw new ParseException("ipfilter must be an json object", ex);
            }
        }

        private Optional<SemuxIpFilter> parseRules(JsonArray rules) {
            return Optional.of(rules.stream()
                    .sequential()
                    .map(this::validateRule)
                    .collect(Collector.of(
                            Builder::new,
                            this::parseRule,
                            (builder1, builder2) -> {
                                builder1.addAll(builder2);
                                return builder1;
                            },
                            Builder::build)));
        }

        private JsonObject validateRule(JsonValue rule) {
            try {
                JsonObject ruleObject = rule.asJsonObject();

                JsonString type = ruleObject.getJsonString("type");
                if (type == null) {
                    throw new ParseException(String.format("type field doesn't exist in rule %s", rule.toString()));
                }

                JsonString address = ruleObject.getJsonString("address");
                if (address == null) {
                    throw new ParseException(String.format("address field doesn't exist in rule %s", rule.toString()));
                }

                return ruleObject;
            } catch (ClassCastException ex) {
                throw new ParseException(String.format("rule %s is not an object", rule.toString()));
            }
        }

        private void parseRule(Builder builder, JsonObject rule) {
            String type = rule.getString("type");
            String address = rule.getString("address");
            try {
                if (type.equals(IpFilterRuleType.ACCEPT.toString())) {
                    logger.info("Loaded rule: ACCEPT {}", address);
                    builder.accept(address);
                } else if (type.equals(IpFilterRuleType.REJECT.toString())) {
                    logger.info("Loaded rule: REJECT {}", address);
                    builder.reject(address);
                } else {
                    throw new ParseException("Rule type of ip filter must be either ACCEPT or REJECT");
                }
            } catch (UnknownHostException ex) {
                throw new ParseException(String.format("Invalid address %s", address), ex);
            }
        }
    }
}
