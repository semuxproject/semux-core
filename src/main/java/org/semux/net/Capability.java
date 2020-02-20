/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import org.semux.net.msg.ReasonCode;

/**
 * This enum represents the available capabilities in current version of Semux
 * wallet. One peer should be disconnected by
 * ${@link ReasonCode#BAD_NETWORK_VERSION} if the peer doesn't support the
 * required set of capabilities.
 */
public enum Capability {

    // NOTE: legacy issues
    // 1) Different names have been used historically;
    // 2) Old handshake messages first converts String to Capability, and then to
    // Capability set;
    // 3) Unknown capability will lead to INVALID_HANDSHAKE;
    // 4) Unsorted capability set will lead to INVALID_HANDSHAKE.

    /**
     * Mandatory for all network.
     */
    SEMUX,

    /**
     * This client supports the FAST_SYNC protocol.
     */
    FAST_SYNC,

    /**
     * This client supports the LIGHT protocol.
     */
    LIGHT;

    public static Capability of(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

}
