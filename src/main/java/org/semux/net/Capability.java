/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import org.semux.net.msg.ReasonCode;

/**
 * This enum represents the available capabilities in current version of Semux
 * wallet.  One peer should be disconnected by ${@link ReasonCode#INCOMPATIBLE_PROTOCOL}
 * if the peer doesn't support the required set of capabilities.
 */
public enum Capability {

    /**
     * A mandatory capability of Semux mainnet.
     */
    SEM,

    /**
     * A mandatory capability of Semux testnet.
     */
    SEM_TESTNET;

    // TODO: BATCH_SYNC

    // TODO: FAST_SYNC

    // TODO: DAPP

    /**
     * The maximum number of capabilities that can be supported.
     */
    public static final int MAX_NUMBER_OF_CAPABILITIES = 128;
}
