/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class IpSingleFilterRule implements IpFilterRule {

    private static final InetAddressValidator inetAddressValidator = new InetAddressValidator();

    private final InetAddress address;

    private final IpFilterRuleType type;

    public IpSingleFilterRule(String ip, IpFilterRuleType type) throws UnknownHostException {
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
}
