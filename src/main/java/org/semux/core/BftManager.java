/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.net.Channel;
import org.semux.net.msg.Message;

public interface BftManager {
    /**
     * Starts bft manager.
     * 
     */
    void start();

    /**
     * Stops bft manager.
     */
    void stop();

    /**
     * Returns if the bft manager is running.
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
     */
    void onMessage(Channel channel, Message msg);
}
