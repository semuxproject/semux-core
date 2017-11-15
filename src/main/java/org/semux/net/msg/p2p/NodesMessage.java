/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class NodesMessage extends Message {

    private Set<InetSocketAddress> nodes;

    /**
     * Create a NODES message.
     * 
     * @param nodes
     */
    public NodesMessage(Set<InetSocketAddress> nodes) {
        super(MessageCode.NODES, null);

        this.nodes = nodes;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(nodes.size());
        for (InetSocketAddress addr : nodes) {
            enc.writeString(addr.getAddress().getHostAddress());
            enc.writeInt(addr.getPort());
        }
        this.encoded = enc.toBytes();
    }

    /**
     * Parse a NODES message from byte array.
     * 
     * @param encoded
     */
    public NodesMessage(byte[] encoded) {
        super(MessageCode.NODES, null);

        this.encoded = encoded;

        nodes = new HashSet<>();
        SimpleDecoder dec = new SimpleDecoder(encoded);
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            String host = dec.readString();
            int port = dec.readInt();
            nodes.add(new InetSocketAddress(host, port));
        }
    }

    public Set<InetSocketAddress> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return "NodesMessage [# nodes =" + nodes.size() + "]";
    }
}
