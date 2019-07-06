/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CommandParserTest {

    @Test
    public void testDelimiting() {
        String input = "foo and \"bar stuff\" \"stuff \\\" with quotes \\\" \" more \"\"";
        List<String> parsed = CommandParser.parseInput(input);

        Assert.assertEquals(6, parsed.size());
        Assert.assertEquals("foo", parsed.get(0));
        Assert.assertEquals("and", parsed.get(1));
        Assert.assertEquals("bar stuff", parsed.get(2));
        Assert.assertEquals("stuff \" with quotes \" ", parsed.get(3));
        Assert.assertEquals("more", parsed.get(4));
        Assert.assertEquals("", parsed.get(5));
    }

    @Test
    public void testBasic() {
        String input = "getBlockByNumber 1";
        List<String> parsed = CommandParser.parseInput(input);
        Assert.assertEquals(2, parsed.size());
        Assert.assertEquals("getBlockByNumber", parsed.get(0));
        Assert.assertEquals("1", parsed.get(1));
    }
}
