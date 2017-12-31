/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.semux.net.msg.p2p.NodesMessage.MAX_NODES;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semux.net.NodeManager;
import org.semux.util.SimpleEncoder;

public class NodesMessageTest {

    @Test
    public void testCodec() {
        Set<NodeManager.Node> nodes = new HashSet<>();
        nodes.add(new NodeManager.Node(new InetSocketAddress("127.0.0.1", 5160)));
        nodes.add(new NodeManager.Node(new InetSocketAddress("127.0.0.1", 5161)));
        NodesMessage nodesMessage = new NodesMessage(new NodesMessage(nodes).getEncoded());
        assertArrayEquals(nodes.toArray(), nodesMessage.getNodes().toArray());
    }

    @Test
    public void testOverflow() {
        MaliciousNodesMessage maliciousNodesMessage = new MaliciousNodesMessage();
        byte[] bytes = maliciousNodesMessage.getEncoded();
        NodesMessage nodesMessage = new NodesMessage(bytes);
        assertEquals(MAX_NODES, nodesMessage.getNodes().size());
    }

    private class MaliciousNodesMessage extends NodesMessage {

        public MaliciousNodesMessage() {
            super();
            SimpleEncoder enc = new SimpleEncoder();
            enc.writeInt(MAX_NODES + 1);
            for (int i = 0; i < MAX_NODES + 1; i++) {
                enc.writeString("www.google.com");
                enc.writeInt(i + 1);
            }
            this.encoded = enc.toBytes();
        }
    }
}
