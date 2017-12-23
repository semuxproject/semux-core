/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.Console;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.zafarkhaja.semver.Version;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SequentialDnsServerAddressStreamProvider;
import oshi.SystemInfo;

public class SystemUtil {
    private static final Logger logger = LoggerFactory.getLogger(SystemUtil.class);

    public static final Scanner SCANNER = new Scanner(System.in);

    public enum OsName {
        WINDOWS("Windows"),

        LINUX("Linux"),

        MACOS("macOS"),

        UNKNOWN("Unknown");

        private String name;

        OsName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Returns the operating system name.
     * 
     * @return
     */
    public static OsName getOsName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return OsName.WINDOWS;
        } else if (os.contains("linux")) {
            return OsName.LINUX;
        } else if (os.contains("mac")) {
            return OsName.MACOS;
        } else {
            return OsName.UNKNOWN;
        }
    }

    /**
     * Returns the operating system architecture
     * 
     * @return
     */
    public static String getOsArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Returns the public IP address of this peer by querying OpenDNS.
     * 
     * @return an IP address if available, otherwise local address
     */
    public static String getIp() {
        try {
            DnsNameResolver nameResolver = new DnsNameResolverBuilder(new NioEventLoopGroup().next())
                    .channelType(NioDatagramChannel.class)
                    .queryTimeoutMillis(1000)
                    .nameServerProvider(new SequentialDnsServerAddressStreamProvider(
                            new InetSocketAddress("208.67.222.222", 53),
                            new InetSocketAddress("208.67.220.220", 53),
                            new InetSocketAddress("208.67.222.220", 53),
                            new InetSocketAddress("208.67.220.222", 53)))
                    .build();
            return nameResolver.resolve("myip.opendns.com").await().get().getHostAddress();
        } catch (Exception e) {
            // no stack trace to avoid too many logs when the wallet goes offline
            logger.error("Failed to retrieve your IP address from opendns");
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            // last chance
            return InetAddress.getLoopbackAddress().getHostAddress();
        }
    }

    /**
     * Reads a password from console with a customized message.
     *
     * @param prompt
     *            A message to display before reading password
     * @return
     */
    public static String readPassword(String prompt) {
        Console console = System.console();

        if (console == null) {
            System.out.print(prompt);
            System.out.flush();

            return SCANNER.nextLine();
        }

        return new String(console.readPassword(prompt));
    }

    /**
     * Reads a password from console.
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

        // check JVM with best effort
        String model = System.getProperty("sun.arch.data.model");
        if (model != null && model.contains("32")) {
            logger.info("You're running 32-bit JVM. Please consider upgrading to 64-bit JVM");
            return false;
        }

        // check CPU
        if (rt.availableProcessors() < 2) {
            logger.info("# of CPU cores = {}", rt.availableProcessors());
            return false;
        }

        // check memory
        if (rt.maxMemory() < 0.8 * 4 * 1024 * 1024 * 1024) {
            logger.info("Max allowed JVM heap memory size = {} MB", rt.maxMemory() / 1024 / 1024);
            return false;
        }

        return true;

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

    /**
     * Returns the number of processors.
     * 
     * @return
     */
    public static int getNumberOfProceessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Returns the available physical memory size in bytes
     * 
     * @return
     */
    public static long getAvailableMemorySize() {
        SystemInfo systemInfo = new SystemInfo();
        return systemInfo.getHardware().getMemory().getAvailable();
    }

    /**
     * Returns the size of heap in use.
     * 
     * @return
     */
    public static long getUsedHeapSize() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private SystemUtil() {
    }
}
