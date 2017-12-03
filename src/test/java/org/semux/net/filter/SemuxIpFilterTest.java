/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import io.netty.handler.ipfilter.IpFilterRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class SemuxIpFilterTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws UnknownHostException {
        return Arrays.asList(new Object[][] {
                // empty filter
                {
                        new SemuxIpFilter.Builder().build(),
                        new HashMap<String, Boolean>() {
                            {
                                put("127.0.0.1", false);
                                put("8.8.8.8", false);
                                put("0:0:0:0:0:0:0:1", false);
                                put("2001:4860:4860::8888", false);
                            }
                        }
                },

                // default route
                {
                        new SemuxIpFilter.Builder()
                                .accept("0.0.0.0/0")
                                .accept("::/0")
                                .build(),
                        new HashMap<String, Boolean>() {
                            {
                                put("127.0.0.1", false);
                                put("8.8.8.8", false);
                                put("0:0:0:0:0:0:0:1", false);
                                put("2001:4860:4860::8888", false);
                            }
                        }
                },

                // whitelist
                {
                        new SemuxIpFilter.Builder()
                                .accept("127.0.0.1")
                                .reject("0.0.0.0/0")
                                .reject("::/0")
                                .build(),
                        new HashMap<String, Boolean>() {
                            {
                                put("127.0.0.1", false);
                                put("8.8.8.8", true);
                                put("0:0:0:0:0:0:0:1", true);
                                put("2001:4860:4860::8888", true);
                            }
                        }
                },

                // blacklist
                {
                        new SemuxIpFilter.Builder()
                                .reject("127.0.0.1")
                                .build(),
                        new HashMap<String, Boolean>() {
                            {
                                put("127.0.0.1", true);
                                put("8.8.8.8", false);
                                put("0:0:0:0:0:0:0:1", false);
                                put("2001:4860:4860::8888", false);
                            }
                        }
                }
        });
    }

    SemuxIpFilter filter;

    HashMap<String, Boolean> isBlocked;

    public SemuxIpFilterTest(SemuxIpFilter filter, HashMap<String, Boolean> isBlocked) {
        this.filter = filter;
        this.isBlocked = isBlocked;
    }

    @Test
    public void testIsBlocked() throws UnknownHostException {
        for (Map.Entry<String, Boolean> entry : isBlocked.entrySet()) {
            assertTrue(filter.isBlocked(mockAddress(entry.getKey())) == entry.getValue());
        }
    }

    private static InetSocketAddress mockAddress(String ip) throws UnknownHostException {
        InetSocketAddress address = mock(InetSocketAddress.class);
        when(address.getAddress()).thenReturn(InetAddress.getByName(ip));
        return address;
    }
}
