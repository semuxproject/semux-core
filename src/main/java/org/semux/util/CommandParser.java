/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    public static List<String> parseInput(String input) {
        List<String> commandArguments = new ArrayList<>();
        Pattern ptrn = Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+");
        Matcher matcher = ptrn.matcher(input);
        while (matcher.find()) {
            commandArguments.add(sanitize(matcher.group(0)));
        }
        return commandArguments;
    }

    private static String sanitize(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\"", "\"");
    }
}
