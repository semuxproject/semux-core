/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

public class Constants {

    /**
     * Default data directory.
     */
    public static final String DEFAULT_DATA_DIR = ".";

    /**
     * Main network ID.
     */
    public static final byte MAIN_NET_ID = 0;

    /**
     * Test network ID.
     */
    public static final byte TEST_NET_ID = 1;

    /**
     * Dev network ID.
     */
    public static final byte DEV_NET_ID = 2;

    /**
     * Name of this client.
     */
    public static final String CLIENT_NAME = "Semux";

    /**
     * Version of this client.
     */
    public static final String CLIENT_VERSION = "1.0.0-rc.3";

    /**
     * Algorithm name for the 256-bit hash.
     */
    public static final String HASH_ALGO = "BLAKE2B-256";

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

    private Constants() {
    }
}
