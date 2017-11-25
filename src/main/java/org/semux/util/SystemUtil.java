/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.zafarkhaja.semver.Version;

public class SystemUtil {
    private static final Logger logger = LoggerFactory.getLogger(SystemUtil.class);

    public static final Scanner SCANNER = new Scanner(System.in);

    /**
     * Get the operating system name.
     * 
     * @return
     */
    public static String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "Windows";
        } else if (os.contains("linux")) {
            return "Linux";
        } else if (os.contains("mac")) {
            return "MacOS";
        } else {
            return "Unkown";
        }
    }

    /**
     * Get the operating system architecture
     * 
     * @return
     */
    public static String getOSArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Get the IP address of this peer.
     * 
     * @return the public IP address if available, otherwise local address
     */
    public static String getIp() {
        try {
            URL url = new URL("https://api.semux.org/ip");
            URLConnection con = url.openConnection();
            con.addRequestProperty("User-Agent", "Mozilla/4.0");
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String ip = reader.readLine().trim();
            reader.close();

            if (ip.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                return ip;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve your IP address", e);
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            // last chance
            return InetAddress.getLoopbackAddress().getHostAddress();
        }
    }

    /**
     * Read a password from console with a customized message.
     * 
     * @return
     */
    public static String readPassword(String message) {
        Console console = System.console();

        if (console == null) {
            System.out.print(message);
            System.out.flush();

            return SCANNER.nextLine();
        }

        return new String(console.readPassword(message));
    }

    /**
     * Read a password from console.
     *
     * @return
     */
    public static String readPassword() {
        return readPassword("Please enter your password: ");
    }

    /**
     * Compare two version strings.
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static int compareVersion(String v1, String v2) {
        return Version.valueOf(v1).compareTo(Version.valueOf(v2));
    }

    /**
     * Benchmarks the host system.
     * 
     * @return
     */
    public static boolean bench() {
        Runtime rt = Runtime.getRuntime();
        logger.debug("# cores = {}, max memory = {}", rt.availableProcessors(), rt.maxMemory());

        return rt.availableProcessors() >= 2 && rt.maxMemory() >= 0.8 * 4 * 1024 * 1024 * 1024;
    }

    /**
     * Terminates the JVM synchronously.
     */
    public static void exit(int code) {
        System.exit(code);
    }

    /**
     * Terminates the JVM asynchronously.
     */
    public static void exitAsync(int code) {
        new Thread(() -> System.exit(code)).start();
    }
}
