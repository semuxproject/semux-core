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

import org.semux.net.NodeManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class NodesMessage extends Message {

    public static final int MAX_NODES = 128;

    private Set<NodeManager.Node> nodes;

    public NodesMessage() {
        super(MessageCode.NODES, null);
    }

    /**
     * Create a NODES message.
     * 
     * @param nodes
     */
    public NodesMessage(Set<NodeManager.Node> nodes) {
        super(MessageCode.NODES, null);

        this.nodes = nodes;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(nodes.size());
        for (InetSocketAddress a : nodes) {
            enc.writeString(a.getAddress().getHostAddress());
            enc.writeInt(a.getPort());
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
        int n = Math.min(dec.readInt(), MAX_NODES);
        for (int i = 0; i < n; i++) {
            String host = dec.readString();
            int port = dec.readInt();
            nodes.add(new NodeManager.Node(host, port));
        }
    }

    public Set<NodeManager.Node> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return "NodesMessage [# nodes =" + nodes.size() + "]";
    }
}
