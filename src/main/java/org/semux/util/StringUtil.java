/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

public class StringUtil {
    /**
     * Convert a string into lower case, except the first letter.
     * 
     * @param str
     * @return
     */
    public static String toLowercaseExceptFirst(String str) {
        char[] chars = str.toLowerCase().toCharArray();
        if (chars.length > 0) {
            chars[0] = Character.toUpperCase(chars[0]);
        }

        return new String(chars);
    }
}
