package org.semux.utils;

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
