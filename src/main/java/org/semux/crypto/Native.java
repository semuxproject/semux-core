/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import org.semux.util.SystemUtil;

/**
 * Semux crypto native implementation
 */
public class Native {

    /**
     * Initialize the native libraries
     */
    private static void init() {
        SystemUtil.OsName os = SystemUtil.getOsName();
        switch (os) {
        case LINUX:
            break;
        case WINDOWS:
            break;
        case MACOS:
            break;
        }
    }

    static {
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
