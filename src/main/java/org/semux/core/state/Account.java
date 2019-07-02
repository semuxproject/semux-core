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
