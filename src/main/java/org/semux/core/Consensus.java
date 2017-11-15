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
     * Start consensus.
     * 
     */
    public void start();

    /**
     * Stop consensus.
     */
    public void stop();

    /**
     * check if this consenus is running.
     * 
     * @return
     */
    public boolean isRunning();

    /**
     * Callback when a message is received from network.
     * 
     * @param channel
     *            the channel where the message is coming from
     * @param msg
     *            the message
     * @return true if the message is processed, otherwise false
     */
    public boolean onMessage(Channel channel, Message msg);
}
