/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.validator.routines.InetAddressValidator;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;

public class SingleIpFilterRule implements IpFilterRule {

    private static final InetAddressValidator inetAddressValidator = new InetAddressValidator();

    private final InetAddress address;

    private final IpFilterRuleType type;

    public SingleIpFilterRule(String ip, IpFilterRuleType type) throws UnknownHostException {
        if (!inetAddressValidator.isValid(ip)) {
            throw new IllegalArgumentException(String.format("Invalid IP %s", ip));
        }

        this.address = InetAddress.getByName(ip);
        this.type = type;
    }

    @Override
    public boolean matches(InetSocketAddress remoteAddress) {
        return address.equals(remoteAddress.getAddress());
    }

    @Override
    public IpFilterRuleType ruleType() {
        return type;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SingleIpFilterRule))
            return false;
        SingleIpFilterRule rule = (SingleIpFilterRule) object;
        return rule.type.equals(this.type) && rule.address.equals(this.address);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(173, 211).append(address).append(type).toHashCode();
    }
}
