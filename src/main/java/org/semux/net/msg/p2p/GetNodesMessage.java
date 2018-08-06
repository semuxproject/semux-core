/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleEncoder;

// NOTE: GetNodesMessage is encoded into a single empty frame.

public class GetNodesMessage extends Message {

    /**
     * Create a GET_NODES message.
     *
     */
    public GetNodesMessage() {
        super(MessageCode.GET_NODES, NodesMessage.class);

        SimpleEncoder enc = new SimpleEncoder();
        this.encoded = enc.toBytes();
    }

    /**
     * Parse a GET_NODES message from byte array.
     * 
     * @param encoded
     */
    public GetNodesMessage(byte[] encoded) {
        super(MessageCode.GET_NODES, NodesMessage.class);

        this.encoded = encoded;
    }

    @Override
    public String toString() {
        return "GetNodesMessage";
    }
}
