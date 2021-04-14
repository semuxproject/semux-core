/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Semux crypto native implementation
 */
public class Native {

    private static final Logger logger = LoggerFactory.getLogger(Native.class);

    protected static File nativeDir;
    protected static boolean enabled = false;

    /**
     * Initializes the native libraries
     */
    protected static void init() {
        if (SystemUtil.is32bitJvm()) {
            // No more support for 32-bit systems
            return;
        }

        SystemUtil.OsName os = SystemUtil.getOsName();
        switch (os) {
        case LINUX:
            if (SystemUtil.getOsArch().equals("aarch64")) {
                enabled = loadLibrary("/native/Linux-aarch64/libsemuxcrypto.so");
            } else {
                enabled = loadLibrary("/native/Linux-x86_64/libsemuxcrypto.so");
            }
            break;
        case WINDOWS:
            enabled = loadLibrary("/native/Windows-x86_64/libsemuxcrypto.dll");
            break;
        default:
            break;
        }
    }

    /**
     * Loads a library file from bundled resource.
     *
     * @param resource
     * @return
     */
    protected static boolean loadLibrary(String resource) {
        try {
            if (nativeDir == null) {
                nativeDir = Files.createTempDirectory("native").toFile();
                nativeDir.deleteOnExit();
            }

            String name = resource.contains("/") ? resource.substring(resource.lastIndexOf('/') + 1) : resource;
            File file = new File(nativeDir, name);

            if (!file.exists()) {
                InputStream in = Native.class.getResourceAsStream(resource); // null pointer exception
                OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                for (int c; (c = in.read()) != -1;) {
                    out.write(c);
                }
                out.close();
                in.close();
            }

            System.load(file.getAbsolutePath());
            return true;
        } catch (Exception | UnsatisfiedLinkError e) {
            logger.warn("Failed to load native library: {}", resource, e);
            return false;
        }
    }

    // initialize library when the class loads
    static {
        init();
    }

    /**
     * Returns whether the native library is enabled.
     *
     * @return
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Disables native implementation.
     */
    public static void disable() {
        enabled = false;
    }

    /**
     * Enables native implementation.
     */
    public static void enable() {
        init();
    }

    /**
     * Computes the 256-bit hash. See {@link Hash#h256(byte[])}
     *
     * @param data
     * @return
     */
    public static native byte[] h256(byte[] data);

    /**
     * Computes the 160-bit hash. See {@link Hash#h160(byte[])}
     *
     * @param data
     * @return
     */
    public static native byte[] h160(byte[] data);

    /**
     * Signs an message using the given key.
     *
     * @param message
     * @param privateKey
     * @return
     */
    public static native byte[] sign(byte[] message, byte[] privateKey);

    /**
     * Verifies an Ed25519 signature.
     *
     * @param message
     * @param signature
     * @param publicKey
     * @return
     */
    public static native boolean verify(byte[] message, byte[] signature, byte[] publicKey);

    /**
     * Batch verifies Ed25519 signatures.
     *
     * @param messages
     * @param signatures
     * @param publicKeys
     * @return
     */
    public static native boolean verifyBatch(byte[][] messages, byte[][] signatures, byte[][] publicKeys);
}
