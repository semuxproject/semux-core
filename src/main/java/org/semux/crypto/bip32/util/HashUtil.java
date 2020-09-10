/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip32.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.semux.crypto.CryptoException;

/**
 * Basic hash functions
 */
public class HashUtil {

    /**
     * SHA-256
     *
     * @param input
     *            input
     * @return sha256(input)
     */
    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Unable to find SHA-256", e);
        }
    }

    /**
     * SHA-512
     *
     * @param input
     *            input
     * @return sha512(input)
     */
    public static byte[] sha512(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Unable to find SHA-512", e);
        }
    }

    /**
     * sha256(sha256(bytes))
     *
     * @param bytes
     *            input
     * @return sha'd twice result
     */
    public static byte[] sha256Twice(byte[] bytes) {
        return sha256Twice(bytes, 0, bytes.length);
    }

    public static byte[] sha256Twice(final byte[] bytes, final int offset, final int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes, offset, length);
            digest.update(digest.digest());
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Unable to find SHA-256", e);
        }
    }

    /**
     * H160
     *
     * @param input
     *            input
     * @return h160(input)
     */
    public static byte[] h160(byte[] input) {
        byte[] sha256 = sha256(input);

        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(sha256, 0, sha256.length);
        byte[] out = new byte[20];
        digest.doFinal(out, 0);
        return out;
    }
}
