/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.crypto.EdDSA;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;

public interface Consensus {
    /**
     * Initialize this consensus manager. Be sure to call this method before
     * starting consensus.
     * 
     * @param chain
     * @param channelMgr
     * @param pendingMgr
     * @param coinbase
     */
    void init(Blockchain chain, ChannelManager channelMgr, PendingManager pendingMgr, EdDSA coinbase);

    /**
     * Starts consensus.
     * 
     */
    void start();

    /**
     * Stops consensus.
     */
    void stop();

    /**
     * Returns if the consenus is running.
     * 
     * @return
     */
    boolean isRunning();

    /**
     * Callback when a message is received from network.
     * 
     * @param channel
     *            the channel where the message is coming from
     * @param msg
     *            the message
     * @return true if the message is processed, otherwise false
     */
    boolean onMessage(Channel channel, Message msg);
}
