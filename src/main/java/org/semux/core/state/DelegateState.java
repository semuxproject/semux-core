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
     * Registers a delegate.
     * 
     * @param addres
     * @param name
     * @param registeredAt
     * @return
     */
    public boolean register(byte[] addres, byte[] name, long registeredAt);

    /**
     * Registers a delegate.
     * 
     * @param address
     * @param name
     * @return
     */
    public boolean register(byte[] address, byte[] name);

    /**
     * Adds vote to a delegate.
     * 
     * @param voter
     * @param delegate
     * @param value
     * 
     * @return
     */
    public boolean vote(byte[] voter, byte[] delegate, long value);

    /**
     * Removes vote of a delegate.
     * 
     * @param voter
     * @param delegate
     * @param value
     * 
     * @return
     */
    public boolean unvote(byte[] voter, byte[] delegate, long value);

    /**
     * Returns vote that one voter has given to the specified delegate.
     * 
     * @param voter
     * @param delegate
     * @return
     */
    public long getVote(byte[] voter, byte[] delegate);

    /**
     * Retrieves delegate by its name.
     * 
     * @param name
     * @return
     */
    public Delegate getDelegateByName(byte[] name);

    /**
     * Retrieves delegate by its address.
     * 
     * @param address
     * @return
     */
    public Delegate getDelegateByAddress(byte[] address);

    /**
     * Returns all delegates.
     * 
     * @return
     */
    public List<Delegate> getDelegates();

    /**
     * Returns a snapshot and starts tracking updates.
     */
    public DelegateState track();

    /**
     * Commits all updates since last snapshot.
     */
    public void commit();

    /**
     * Reverts all updates since last snapshot.
     */
    public void rollback();
}
