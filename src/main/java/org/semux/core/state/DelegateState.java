/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import java.util.List;

import org.semux.core.Delegate;

public interface DelegateState {
    /**
     * Register delegate.
     * 
     * @param delegate
     * @param name
     * @return
     */
    public boolean register(byte[] delegate, byte[] name);

    /**
     * Vote for a delegate.
     * 
     * @param voter
     * @param delegate
     * @param value
     * 
     * @return
     */
    public boolean vote(byte[] voter, byte[] delegate, long value);

    /**
     * Revoke a previous vote for a delegate.
     * 
     * @param voter
     * @param delegate
     * @param value
     * 
     * @return
     */
    public boolean unvote(byte[] voter, byte[] delegate, long value);

    /**
     * Get the vote that one voter has given to the specified delegate.
     * 
     * @param voter
     * @param delegate
     * @return
     */
    public long getVote(byte[] voter, byte[] delegate);

    /**
     * Get delegate by its name.
     * 
     * @param name
     * @return
     */
    public Delegate getDelegateByName(byte[] name);

    /**
     * Get delegate by its address.
     * 
     * @param addr
     * @return
     */
    public Delegate getDelegateByAddress(byte[] addr);

    /**
     * Get the validator set based on current state, sorted by votes.
     * 
     * @return
     */
    public List<Delegate> getDelegates();

    /**
     * Make a snapshot and start tracking updates.
     */
    public DelegateState track();

    /**
     * Commit all updates since last snapshot.
     */
    public void commit();

    /**
     * Revert all updates since last snapshot.
     */
    public void rollback();
}
