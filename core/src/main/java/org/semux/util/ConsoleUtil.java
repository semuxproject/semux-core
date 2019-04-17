/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Scanner;

public class ConsoleUtil {

    private static final Scanner scanner = new Scanner(new InputStreamReader(System.in, Charset.defaultCharset()));

    private ConsoleUtil() {
    }

    /**
     * Reads a line from the console.
     *
     * @param prompt
     * @return
     */
    public static String readLine(String prompt) {
        if (prompt != null) {
            System.out.print(prompt);
            System.out.flush();
        }

        return scanner.nextLine();
    }

    /**
     * Reads a password from the console.
     *
     * @param prompt
     *            A message to display before reading password
     * @return
     */
    public static String readPassword(String prompt) {
        Console console = System.console();

        if (console == null) {
            if (prompt != null) {
                System.out.print(prompt);
                System.out.flush();
            }

            return scanner.nextLine();
        }

        return new String(console.readPassword(prompt));
    }

    /**
     * Reads a password from the console.
     *
     * @return
     */
    public static String readPassword() {
        return readPassword("Please enter your password: ");
    }
}
