/**
 * Copyright (c) 2017-2018 The Semux Developers
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
        SystemUtil.OsName os = SystemUtil.getOsName();
        switch (os) {
        case LINUX:
            if (SystemUtil.is32bitJvm()) {
                enabled = loadLibrary("/native/linux32/libsodium.so.23") && loadLibrary("/native/linux32/libcrypto.so");
            } else {
                enabled = loadLibrary("/native/linux64/libsodium.so.23") && loadLibrary("/native/linux64/libcrypto.so");
            }
            break;
        case MACOS:
            enabled = loadLibrary("/native/macos/libsodium.23.dylib") && loadLibrary("/native/macos/libcrypto.dylib");
            break;
        case WINDOWS:
            if (SystemUtil.is32bitJvm()) {
                enabled = loadLibrary("/native/win32/libsodium.dll") && loadLibrary("/native/win32/crypto.dll");
            } else {
                enabled = loadLibrary("/native/win64/libsodium.dll") && loadLibrary("/native/win64/crypto.dll");
            }
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

            String name = resource.contains("/") ? resource.substring(resource.lastIndexOf('/') + 1, resource.length())
                    : resource;
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
     * Computes the blake2b hash.
     *
     * @param data
     * @return
     */
    public static native byte[] blake2b(byte[] data);

    /**
     * Creates an Ed25519 message signature with the given private key.
     *
     * @param message
     * @param privateKey
     * @return
     */
    public static native byte[] ed25519_sign(byte[] message, byte[] privateKey);

    /**
     * Verifies an Ed25519 signature.
     *
     * @param message
     * @param signature
     * @param publicKey
     * @return
     */
    public static native boolean ed25519_verify(byte[] message, byte[] signature, byte[] publicKey);
}
