/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.semux.net.filter.exception.ParseException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;

/**
 * FilterRule is a proxy class of {@link IpSubnetFilterRule} and
 * {@link IpSingleFilterRule}
 */
public class FilterRule implements IpFilterRule {

    private static final Pattern CIDR_PATTERN = Pattern
            .compile("^(?<address>[0-9.a-fA-F:]+?)(/(?<cidrPrefix>\\d{1,3}))?$");

    private static final InetAddressValidator inetAddressValidator = new InetAddressValidator();

    /**
     * The actual instance of IpFilterRule to be matched against of.
     */
    private final IpFilterRule ipFilterRule;

    private final IpFilterRuleType ruleType;

    /**
     * FilterRule constructor decides on the type of IpFilterRule based the provided
     * address parameter.
     * <p>
     * The following cases are handled:
     * <ul>
     * <li>CIDR Notation: {@link IpSubnetFilterRule}</li>
     * <li>IP Address: {@link IpSingleFilterRule}</li>
     * </ul>
     * 
     * @param address
     *            An IP address or a CIDR notation
     * @param ruleType
     *            ACCEPT or REJECT
     * @throws UnknownHostException
     */
    public FilterRule(String address, IpFilterRuleType ruleType) throws UnknownHostException {
        this.ruleType = ruleType;

        Matcher matcher = CIDR_PATTERN.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid CIDR notation or IP address");
        }

        address = matcher.group("address");
        if (!inetAddressValidator.isValid(address)) {
            throw new IllegalArgumentException(String.format("%s is not a valid ip address", address));
        }

        if (matcher.group("cidrPrefix") != null) {
            int cidrPrefix = Integer.parseInt(matcher.group("cidrPrefix"));
            ipFilterRule = new IpSubnetFilterRule(address, cidrPrefix, ruleType);
        } else {
            ipFilterRule = new IpSingleFilterRule(address, ruleType);
        }
    }

    @Override
    public boolean matches(InetSocketAddress remoteAddress) {
        return ipFilterRule.matches(remoteAddress);
    }

    @Override
    public IpFilterRuleType ruleType() {
        return ruleType;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FilterRule))
            return false;
        FilterRule rule = (FilterRule) object;
        return rule.ruleType.equals(this.ruleType) && rule.ipFilterRule.equals(this.ipFilterRule);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(79, 103).append(ipFilterRule).append(ruleType).toHashCode();
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static FilterRule jsonCreator(@JsonProperty(value = "type", required = true) String type,
            @JsonProperty(value = "address", required = true) String address) {
        try {
            return new FilterRule(address, IpFilterRuleType.valueOf(type));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ParseException("Rule type of ip filter must be either ACCEPT or REJECT");
        } catch (UnknownHostException ex) {
            throw new ParseException(String.format("Invalid address %s", address), ex);
        }
    }
}
