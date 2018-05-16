/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.security.spec.InvalidKeySpecException;

import org.semux.crypto.CryptoException;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;

public class Constants {

    /**
     * JSON mime type string.
     */
    public static final String JSON_MIME = "application/json";

    /**
     * Default data directory.
     */
    public static final String DEFAULT_DATA_DIR = ".";

    /**
     * Network versions.
     */
    public static final short MAINNET_VERSION = 0;
    public static final short TESTNET_VERSION = 0;
    public static final short DEVNET_VERSION = 0;

    /**
     * Name of this client.
     */
    public static final String CLIENT_NAME = "Semux";

    /**
     * Version of this client.
     */
    public static final String CLIENT_VERSION = "1.2.0";

    /**
     * Algorithm name for the 256-bit hash.
     */
    public static final String HASH_ALGORITHM = "BLAKE2B-256";

    /**
     * Name of the config directory.
     */
    public static final String CONFIG_DIR = "config";

    /**
     * Name of the database directory.
     */
    public static final String DATABASE_DIR = "database";

    /**
     * The default IP port for p2p protocol
     */
    public static final int DEFAULT_P2P_PORT = 5161;

    /**
     * The default IP port for RESTful API.
     */
    public static final int DEFAULT_API_PORT = 5171;

    /**
     * The default user agent for HTTP requests.
     */
    public static final String DEFAULT_USER_AGENT = "Mozilla/4.0";

    /**
     * The default connect timeout.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 4000;

    /**
     * The default read timeout.
     */
    public static final int DEFAULT_READ_TIMEOUT = 4000;

    /**
     * The number of blocks per day.
     */
    public static final long BLOCKS_PER_DAY = 2L * 60L * 24L;

    /**
     * The number of blocks per year.
     */
    public static final long BLOCKS_PER_YEAR = 2L * 60L * 24L * 365L;

    /**
     * The public-private key pair for signing coinbase transactions.
     */
    public static final Key COINBASE_KEY;

    /**
     * Address bytes of {@link this#COINBASE_KEY}. This is stored as a cache to
     * avoid redundant h160 calls.
     */
    public static final byte[] COINBASE_ADDRESS;

    /**
     * The public-private key pair of the genesis validator.
     */
    public static final Key DEVNET_KEY;

    static {
        try {
            COINBASE_KEY = new Key(Hex.decode0x(
                    "0x302e020100300506032b657004220420acdd12174cbc3fa6e4076cb1e270989cf4d47b0de8942c8542fe6a3bed34d7bf"));
            COINBASE_ADDRESS = COINBASE_KEY.toAddress();
            DEVNET_KEY = new Key(Hex.decode0x(
                    "0x302e020100300506032b657004220420acbd5f2cb2b6053f704376d12df99f2aa163d267a755c7f1d9fe55d2a2dc5405"));
        } catch (InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    private Constants() {
    }
}
