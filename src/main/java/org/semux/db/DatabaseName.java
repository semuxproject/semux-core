/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

public enum DatabaseName {

    /**
     * Block and transaction index.
     */
    INDEX,

    /**
     * Block raw data.
     */
    BLOCK,

    /**
     * Account related data.
     */
    ACCOUNT,

    /**
     * Delegate core data.
     */
    DELEGATE,

    /**
     * Delegate vote data.
     */
    VOTE
}