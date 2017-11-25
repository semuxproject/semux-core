/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import java.util.List;
import java.util.Map;

import org.semux.util.ByteArray;

public interface DelegateState {
    /**
     * Registers a delegate.
     * 
     * @param addres
     * @param name
     * @param registeredAt
     * @return
     */
    boolean register(byte[] addres, byte[] name, long registeredAt);

    /**
     * Registers a delegate.
     * 
     * @param address
     * @param name
     * @return
     */
    boolean register(byte[] address, byte[] name);

    /**
     * Adds vote to a delegate.
     * 
     * @param voter
     * @param delegate
     * @param value
     * 
     * @return
     */
    boolean vote(byte[] voter, byte[] delegate, long value);

    /**
     * Removes vote of a delegate.
     * 
     * @param voter
     * @param delegate
     * @param value
     * 
     * @return
     */
    boolean unvote(byte[] voter, byte[] delegate, long value);

    /**
     * Returns vote that one voter has given to the specified delegate.
     * 
     * @param voter
     * @param delegate
     * @return
     */
    long getVote(byte[] voter, byte[] delegate);

    /**
     * Returns all the votes for one delegate.
     *
     * @param delegate
     * @return
     */
    Map<ByteArray, Long> getVotes(byte[] delegate);

    /**
     * Retrieves delegate by its name.
     * 
     * @param name
     * @return
     */
    Delegate getDelegateByName(byte[] name);

    /**
     * Retrieves delegate by its address.
     * 
     * @param address
     * @return
     */
    Delegate getDelegateByAddress(byte[] address);

    /**
     * Returns all delegates.
     * 
     * @return
     */
    List<Delegate> getDelegates();

    /**
     * Returns a snapshot and starts tracking updates.
     */
    DelegateState track();

    /**
     * Commits all updates since last snapshot.
     */
    void commit();

    /**
     * Reverts all updates since last snapshot.
     */
    void rollback();
}
