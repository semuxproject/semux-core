/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Account;

public interface AccountState {
    /**
     * Get an account by its address.
     *
     * @param addr
     * @return the account state
     */
    public Account getAccount(byte[] addr);

    /**
     * Make a snapshot and start tracking updates.
     */
    public AccountState track();

    /**
     * Commit all updates since last snapshot.
     */
    public void commit();

    /**
     * Revert all updates since last snapshot.
     */
    public void rollback();
}
