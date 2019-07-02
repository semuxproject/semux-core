/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.db;

public enum DatabaseName {

    /**
     * @deprecated v0 and v1 only Block and transaction index.
     */
    INDEX,

    /**
     * Block raw data.
     */
    BLOCK,

    /**
     * @deprecated v0 and v1 only Account related data.
     */
    ACCOUNT,

    /**
     * @deprecated v0 and v1 only DelegateV1 core data.
     */
    DELEGATE,

    /**
     * @deprecated v0 and v1 only DelegateV1 vote data.
     */
    VOTE
}