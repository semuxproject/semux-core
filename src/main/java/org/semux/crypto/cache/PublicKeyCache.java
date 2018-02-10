/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.cache;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.semux.crypto.CryptoException;
import org.semux.util.ByteArray;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

public final class PublicKeyCache {

    private static final int MAX_CACHE_SIZE = 16 * 1024;

    /**
     * EdDSAPublicKey constructor consumes ~37% of CPU time of
     * EdDSAPublicKey::verify mainly by creating precomputed tables. Considering
     * that Semux has an infrequently changed list of validators, caching public
     * keys can reduce the synchronization time significantly.
     * <p>
     * The cache is a concurrent hash map of ByteArray.of(pubKey) -> EdDSAPublicKey
     */
    private static final Cache<ByteArray, EdDSAPublicKey> pubKeyCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE).build();

    private PublicKeyCache() {
    }

    /**
     * Returns cached EdDSAPublicKey from its byte array format.
     *
     * @param pubKey
     * @return
     */
    public static EdDSAPublicKey computeIfAbsent(byte[] pubKey) {
        return pubKeyCache.get(ByteArray.of(pubKey), input -> {
            try {
                return new EdDSAPublicKey(new X509EncodedKeySpec(pubKey));
            } catch (InvalidKeySpecException e) {
                throw new CryptoException(e);
            }
        });
    }
}
