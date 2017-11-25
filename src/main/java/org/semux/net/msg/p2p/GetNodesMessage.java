/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import java.net.InetSocketAddress;
import java.util.Set;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.Bytes;

public class GetNodesMessage extends Message {

    private Set<InetSocketAddress> nodes;

    /**
     * Create a GET_NODES message.
     *
     */
    public GetNodesMessage() {
        super(MessageCode.GET_NODES, NodesMessage.class);

        this.encoded = Bytes.EMPTY_BYTES;
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

    public Set<InetSocketAddress> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return "GetNodesMessage";
    }
}
