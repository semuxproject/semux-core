/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.semux.net.msg.p2p.NodesMessage.MAX_NODES;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.semux.net.NodeManager.Node;

public class NodesMessageTest {

    @Test
    public void testCodec() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(new InetSocketAddress("127.0.0.1", 5160)));
        nodes.add(new Node(new InetSocketAddress("127.0.0.1", 5161)));
        NodesMessage nodesMessage = new NodesMessage(new NodesMessage(nodes).getEncoded());
        assertArrayEquals(nodes.toArray(), nodesMessage.getNodes().toArray());
    }

    @Test
    public void testOverflow() {
        List<Node> list = new ArrayList<>();
        for (int i = 0; i < MAX_NODES + 1; i++) {
            list.add(new Node("127.0.0.1", 1234));
        }
        NodesMessage maliciousMessage = new NodesMessage(list);

        assertFalse(maliciousMessage.validate());
    }
}
