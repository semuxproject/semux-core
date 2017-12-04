/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hex;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public final class EdDSAPublicKeyCache {

    /**
     * EdDSAPublicKey constructor consumes ~37% of CPU time of
     * EdDSAPublicKey::verify mainly by creating precomputed tables. Considering
     * that Semux has an infrequently changed list of validators, caching public
     * keys can reduce the synchronization time significantly.
     * <p>
     * The cache is a concurrent hash map of Hex(byte[] publicKey) -> EdDSAPublicKey
     * <p>
     * softValues() allows GC to cleanup cached values automatically.
     */
    private static final Cache<String, EdDSAPublicKey> pubKeyCache = Caffeine.newBuilder().softValues().build();

    private EdDSAPublicKeyCache() {
    }

    /**
     * Returns cached EdDSAPublicKey hashed by hexadecimal public key
     *
     * @param pubKey
     * @return
     */
    public static EdDSAPublicKey computeIfAbsent(byte[] pubKey) {
        return pubKeyCache.get(Hex.encode(pubKey), input -> {
            try {
                return new EdDSAPublicKey(new X509EncodedKeySpec(pubKey));
            } catch (InvalidKeySpecException e) {
                throw new CryptoException(e);
            }
        });
    }
}
