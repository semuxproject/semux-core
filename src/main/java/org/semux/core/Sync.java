/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;

public interface Sync {
    /**
     * Initialize the sync manager.
     * 
     * @param chain
     * @param channelMgr
     */
    public void init(Blockchain chain, ChannelManager channelMgr);

    /**
     * Start sync manager, and sync blocks in [height, targetHeight).
     * 
     * @param targetHeight
     *            the target height, exclusive
     */
    public void start(long targetHeight);

    /**
     * Stop sync manager.
     */
    public void stop();

    /**
     * check if this sync manager is running.
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
