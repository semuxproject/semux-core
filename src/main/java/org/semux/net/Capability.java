/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import org.semux.net.msg.ReasonCode;

/**
 * This enum represents the available capabilities in current version of Semux
 * wallet.
 */
public enum Capability {

    /**
     * A mandatory capability. One peer should be disconnected by
     * ${@link ReasonCode#INCOMPATIBLE_PROTOCOL} if the peer doesn't support this
     * capability.
     */
    SEM;

    // TODO: BATCH_SYNC

    // TODO: FAST_SYNC

    // TODO: DAPP

    /**
     * Creates a ${@link CapabilitySet} contains the currently supported
     * capabilities.
     *
     * @return a ${@link CapabilitySet} contains the currently supported
     *         capabilities.
     */
    public static CapabilitySet supported() {
        return CapabilitySet.of(SEM);
    }
}
