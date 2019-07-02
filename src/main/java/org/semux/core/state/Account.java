/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;

public abstract class Account {

    public abstract byte[] getAddress();

    /**
     * Returns the available balance of this account.
     *
     * @return
     */
    public abstract Amount getAvailable();

    /**
     * Sets the available balance of this account.
     *
     * @param available
     */
    abstract void setAvailable(Amount available);

    /**
     * Returns the locked balance of this account.
     *
     * @return
     */
    public abstract Amount getLocked();

    /**
     * Sets the locked balance of this account.
     *
     * @param locked
     */
    abstract void setLocked(Amount locked);

    /**
     * Gets the nonce of this account.
     *
     * @return
     */
    public abstract long getNonce();

    abstract void setNonce(long nonce);
}
