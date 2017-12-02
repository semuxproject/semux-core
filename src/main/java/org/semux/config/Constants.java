/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

public class Constants {

    /**
     * Main network ID.
     */
    public static final byte ID_MAIN_NET = 0;

    /**
     * Test network ID.
     */
    public static final byte ID_TEST_NET = 1;

    /**
     * Dev network ID.
     */
    public static final byte ID_DEV_NET = 2;

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
    public static final String DIR_CONFIG = "config";

    /**
     * Name of the database directory.
     */
    public static final String DIR_DATABASE = "database";

    /**
     * The default IP port for p2p protocol
     */
    public static final int PORT_P2P = 5161;

    /**
     * The default IP port for RESTful API.
     */
    public static final int PORT_API = 5171;

    private Constants() {
    }
}
