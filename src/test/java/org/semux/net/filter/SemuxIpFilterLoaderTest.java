/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semux.net.filter.exception.ParseException;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;

@RunWith(Parameterized.class)
public class SemuxIpFilterLoaderTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws UnknownHostException {
        return Arrays.asList(new Object[][] { { getFile("empty.json"), new ArrayList<IpFilterRule>(), null },
                { getFile("blacklist.json"),
                        Arrays.asList(new FilterRule("1.2.3.4", IpFilterRuleType.REJECT),
                                new FilterRule("5.6.7.8", IpFilterRuleType.REJECT)),
                        null },
                { getFile("whitelist.json"),
                        Arrays.asList(new FilterRule("127.0.0.1/8", IpFilterRuleType.ACCEPT),
                                new FilterRule("192.168.0.0/16", IpFilterRuleType.ACCEPT),
                                new FilterRule("0.0.0.0/0", IpFilterRuleType.REJECT)),
                        null },
                { getFile("exception_empty_object.json"), null, ParseException.class },
                { getFile("exception_typo_in_rules1.json"), null, ParseException.class },
                { getFile("exception_typo_in_rules2.json"), null, ParseException.class },
                { getFile("exception_typo_in_rules3.json"), null, ParseException.class },
                { getFile("exception_typo_in_rules4.json"), null, ParseException.class },
                { getFile("exception_type_cast1.json"), null, ParseException.class },
                { getFile("exception_type_cast2.json"), null, ParseException.class },
                { getFile("exception_type_cast3.json"), null, ParseException.class }, });
    }

    File jsonFile;

    List<IpFilterRule> rules;

    Class<? extends Throwable> exception;

    public SemuxIpFilterLoaderTest(File jsonFile, List<IpFilterRule> rules, Class<? extends Throwable> exception) {
        this.jsonFile = jsonFile;
        this.rules = rules;
        this.exception = exception;
    }

    private static File getFile(String fileName) {
        return new File(SemuxIpFilterLoaderTest.class.getResource("/ipfilter/" + fileName).getFile());
    }

    @Test
    public void testLoad() {
        if (exception != null) {
            expectedException.expect(exception);
        }

        SemuxIpFilter.Loader loader = new SemuxIpFilter.Loader();
        SemuxIpFilter semuxIpFilter = loader.load(jsonFile.toPath());
        List<FilterRule> loadedRules = semuxIpFilter.getRules();
        assertTrue(loadedRules.size() == rules.size());
        for (int i = 0; i < loadedRules.size(); i++) {
            assertThat(loadedRules.get(i)).isEqualToComparingFieldByFieldRecursively(rules.get(i));
        }
    }
}
