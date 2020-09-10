/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.semux.config.Constants;

/**
 * Hash generator
 */
public class Hash {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final int HASH_LEN = 32;

    /**
     * Generate the 256-bit hash.
     *
     * @param input
     * @return
     */
    public static byte[] h256(byte[] input) {

        if (Native.isEnabled()) {
            return Native.h256(input);
        } else {
            try {
                MessageDigest digest = MessageDigest.getInstance(Constants.HASH_ALGORITHM);
                return digest.digest(input);
            } catch (Exception e) {
                throw new CryptoException(e);
            }
        }
    }

    /**
     * Merge two byte arrays and compute the 256-bit hash.
     *
     * @param one
     * @param two
     * @return
     */
    public static byte[] h256(byte[] one, byte[] two) {
        byte[] all = new byte[one.length + two.length];
        System.arraycopy(one, 0, all, 0, one.length);
        System.arraycopy(two, 0, all, one.length, two.length);

        return Hash.h256(all);
    }

    /**
     * Generate the 160-bit hash, using h256 and RIPEMD.
     *
     * @param input
     * @return
     */
    public static byte[] h160(byte[] input) {
        if (Native.isEnabled()) {
            return Native.h160(input);
        } else {
            try {
                byte[] h256 = h256(input);

                RIPEMD160Digest digest = new RIPEMD160Digest();
                digest.update(h256, 0, h256.length);
                byte[] out = new byte[20];
                digest.doFinal(out, 0);
                return out;
            } catch (Exception e) {
                throw new CryptoException(e);
            }
        }
    }

    private Hash() {
    }

}
