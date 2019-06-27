/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.util.Locale;

import org.semux.config.Constants;
// import org.semux.gui.SemuxGui;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;

import oshi.SystemInfo;

public class SystemUtil {

    // Fix JNA issue: There is an incompatible JNA native library installed
    static {
        System.setProperty("jna.nosys", "true");
    }

    private static final Logger logger = LoggerFactory.getLogger(SystemUtil.class);

    public static class Code {
        // success
        public static final int OK = 0;

        // wallet
        public static final int FAILED_TO_WRITE_WALLET_FILE = 11;
        public static final int FAILED_TO_UNLOCK_WALLET = 12;
        public static final int ACCOUNT_NOT_EXIST = 13;
        public static final int ACCOUNT_ALREADY_EXISTS = 14;
        public static final int INVALID_PRIVATE_KEY = 15;
        public static final int WALLET_LOCKED = 16;
        public static final int PASSWORD_REPEAT_NOT_MATCH = 17;
        public static final int WALLET_ALREADY_EXISTS = 18;
        public static final int WALLET_ALREADY_UNLOCKED = 19;

        // kernel
        public static final int FAILED_TO_INIT_ED25519 = 31;
        public static final int FAILED_TO_LOAD_CONFIG = 32;
        public static final int FAILED_TO_LOAD_GENESIS = 33;
        public static final int FAILED_TO_LAUNCH_KERNEL = 34;
        public static final int INVALID_NETWORK_LABEL = 35;

        // database
        public static final int FAILED_TO_OPEN_DB = 51;
        public static final int FAILED_TO_REPAIR_DB = 52;
        public static final int FAILED_TO_WRITE_BATCH_TO_DB = 53;

        // upgrade
        public static final int HARDWARE_UPGRADE_NEEDED = 71;
        public static final int CLIENT_UPGRADE_NEEDED = 72;
        public static final int JVM_32_NOT_SUPPORTED = 73;
    }

    public enum OsName {
        WINDOWS("Windows"),

        LINUX("Linux"),

        MACOS("macOS"),

        UNKNOWN("Unknown");

        private final String name;

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
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

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
     * Returns whether the JVM is in 32-bit data model
     *
     * @return
     */
    public static boolean is32bitJvm() {
        String model = System.getProperty("sun.arch.data.model");
        return model != null && model.contains("32");
    }

    /**
     * Returns whether the JVM is in 64-bit data model
     *
     * @return
     */
    public static boolean is64bitJvm() {
        String model = System.getProperty("sun.arch.data.model");
        return model != null && model.contains("64");
    }

    /**
     * Returns my public IP address.
     *
     * @return an IP address if available, otherwise local address
     */
    public static String getIp() {
        try {
            URL url = new URL("http://checkip.amazonaws.com/");
            URLConnection con = url.openConnection();
            con.addRequestProperty("User-Agent", Constants.DEFAULT_USER_AGENT);
            con.setConnectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT);
            con.setReadTimeout(Constants.DEFAULT_READ_TIMEOUT);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), UTF_8));
            String ip = reader.readLine().trim();
            reader.close();

            // only IPv4 is supported currently
            if (ip.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                return ip;
            }
        } catch (IOException e1) {
            logger.warn("Failed to retrieve your public IP address");

            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e2) {
                logger.warn("Failed to retrieve your localhost IP address");
            }
        }

        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    /**
     * Compares two version strings.
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
        // check JVM data model
        if (is32bitJvm()) {
            logger.info("You're running 32-bit JVM. Please consider upgrading to 64-bit JVM");
            return false;
        }

        // check CPU
        if (getNumberOfProcessors() < 2) {
            logger.info("# of CPU cores = {}", getNumberOfProcessors());
            return false;
        }

        // check memory
        if (getTotalMemorySize() < 3L * 1024L * 1024L * 1024L) {
            logger.info("Total physical memory size = {} MB", getTotalMemorySize() / 1024 / 1024);
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
    public static int getNumberOfProcessors() {
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
     * Returns the total physical memory size in bytes
     *
     * @return
     */
    public static long getTotalMemorySize() {
        SystemInfo systemInfo = new SystemInfo();
        return systemInfo.getHardware().getMemory().getTotal();
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

    /**
     * Change localization.
     *
     * @param locale
     *            the target localization.
     */
    public static void setLocale(Locale locale) {
        try {
            if (!Locale.getDefault().equals(locale)) {
                Locale.setDefault(locale);
            }
        } catch (SecurityException e) {
            logger.error("Unable to change localization.", e);
        }
    }

    private static String version = null;

    /**
     * Returns the implementation version.
     *
     * @return
     */
    public static Object getImplementationVersion() {
        if (version == null) {
            try {
                version = IOUtil.readStreamAsString(SystemUtil.class.getClassLoader().getResourceAsStream("VERSION"))
                        .trim();
            } catch (IOException ex) {
                logger.info("Failed to read version.");
                version = "unknown";
            }
        }

        return version;
    }

    /**
     * Returns whether Microsoft Visual C++ 2012 Redistributable Package is
     * installed.
     *
     * @return
     */
    public static boolean isWindowsVCRedist2012Installed() {
        if (SystemUtil.getOsName() != OsName.WINDOWS) {
            throw new UnreachableException();
        }

        try {
            if (Platform.is64Bit()) {
                return Advapi32Util.registryGetIntValue(
                        Advapi32Util.registryGetKey(
                                WinReg.HKEY_LOCAL_MACHINE,
                                "SOFTWARE\\Microsoft\\VisualStudio\\11.0\\VC\\Runtimes\\x64",
                                WinNT.KEY_READ | WinNT.KEY_WOW64_32KEY).getValue(),
                        "Installed") == 1;
            } else {
                return Advapi32Util.registryGetIntValue(
                        WinReg.HKEY_LOCAL_MACHINE,
                        "SOFTWARE\\Microsoft\\VisualStudio\\11.0\\VC\\Runtimes\\x86",
                        "Installed") == 1;
            }
        } catch (Win32Exception e) {
            logger.error("Failed to read windows registry", e);
            return false;
        }
    }

    /**
     * Determine if the current Java runtime supports the Java Platform Module
     * System. Credits to: https://github.com/junit-team/
     *
     * @return {@code true} if the Java Platform Module System is available,
     *         otherwise {@code false}
     */
    public static boolean isJavaPlatformModuleSystemAvailable() {
        try {
            Class.forName("java.lang.Module");
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }

    /**
     * Check if current OS is POSIX compliant.
     *
     * @return whether current OS is POSIX compliant
     */
    public static boolean isPosix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    /**
     * Checks if it's running in JUnit test.
     *
     * @return
     */
    public static boolean isJUnitTest() {
        try {
            Class.forName("org.semux.TestUtils");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private SystemUtil() {
    }
}
