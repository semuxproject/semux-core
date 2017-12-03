/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import io.netty.handler.ipfilter.IpFilterRuleType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class CIDRFilterRuleExceptionTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // invalid addresses
                { "127.0.0.1/33", IllegalArgumentException.class },
                { "127.0.0.1/abc", IllegalArgumentException.class },
                { "127.0.0.1/", IllegalArgumentException.class },
                { "127001", IllegalArgumentException.class },
                { "127.0.0", IllegalArgumentException.class },
                { "2001:db8::/255", IllegalArgumentException.class },

                // valid addresses
                { "127.0.0.1", null },
                { "2001:4860:4860::8888", null },
        });
    }

    private String cidrNotation;

    private String matchesIp;

    private Class<? extends Throwable> throwsException;

    public CIDRFilterRuleExceptionTest(String cidrNotation, Class<? extends Throwable> throwsException) {
        this.cidrNotation = cidrNotation;
        this.throwsException = throwsException;
    }

    @Test
    public void testMatches() throws UnknownHostException {
        if (throwsException != null) {
            expectedException.expect(throwsException);
        }

        CIDRFilterRule filterRule = new CIDRFilterRule(cidrNotation, IpFilterRuleType.REJECT);
    }
}
