/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.netty.handler.ipfilter.IpFilterRuleType;

@RunWith(Parameterized.class)
public class FilterRuleTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // ipv4 cidr notations
                { "127.0.0.1/8", "127.1.2.3", true }, { "127.0.0.1/8", "128.0.0.1", false },
                { "192.168.0.0/16", "192.168.1.2", true },

                // ipv4 single ip
                { "127.0.0.1", "127.0.0.1", true }, { "127.0.0.1", "127.0.0.2", false },

                // ipv6 cidr notations
                { "2001:db8::/48", "2001:db8:0:0:0:0:0:0", true }, { "2001:db8::/48", "2001:db8:1:0:0:0:0:0", false },

                // ipv6 single ip
                { "2001:4860:4860::8888", "2001:4860:4860::8888", true },
                { "2001:4860:4860::8888", "2001:4860:4860::8889", false },

                // default route
                { "0.0.0.0/0", "127.0.0.1", true }, { "0.0.0.0/0", "127.0.0.2", true },
                { "::/0", "2001:4860:4860::8888", true }, { "::/0", "2001:4860:4860::8889", true } });
    }

    private String cidrNotation;

    private String matchesIp;

    private boolean matches;

    public FilterRuleTest(String cidrNotation, String matchesIp, boolean matches) {
        this.cidrNotation = cidrNotation;
        this.matchesIp = matchesIp;
        this.matches = matches;
    }

    @Test
    public void testMatches() throws UnknownHostException {
        FilterRule FilterRule = new FilterRule(cidrNotation, IpFilterRuleType.REJECT);
        InetAddress inetAddress = InetAddress.getByName(matchesIp);
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(inetAddress);
        assertEquals(matches, FilterRule.matches(inetSocketAddress));
    }
}
