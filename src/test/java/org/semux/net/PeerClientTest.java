/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import org.junit.Rule;
import org.junit.Test;
import org.semux.KernelMock;
import org.semux.consensus.SemuxBFT;
import org.semux.consensus.SemuxSync;
import org.semux.core.BlockchainImpl;
import org.semux.core.PendingManager;
import org.semux.crypto.EdDSA;
import org.semux.rules.TemporaryDBRule;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PeerClientTest {

    private static final String P2P_IP = "127.0.0.1";
    private static final int P2P_PORT = 15161;

    @Rule
    public TemporaryDBRule temporaryDBFactory = new TemporaryDBRule();

    @Test
    public void testConnect() throws InterruptedException {

        PeerServerMock ps = new PeerServerMock(temporaryDBFactory);
        ps.start(P2P_IP, P2P_PORT);
        assertTrue(ps.getServer().isListening());

        try {
            EdDSA key = new EdDSA();
            PeerClient client = new PeerClient(P2P_IP, P2P_PORT + 1, key);

            KernelMock kernel = new KernelMock();
            kernel.setBlockchain(new BlockchainImpl(kernel.getConfig(), temporaryDBFactory));
            kernel.setClient(client);
            kernel.setChannelManager(new ChannelManager());
            kernel.setPendingManager(new PendingManager(kernel));
            kernel.setNodeManager(new NodeManager(kernel));
            kernel.setConsensus(new SemuxBFT(kernel));
            kernel.setSyncManager(new SemuxSync(kernel));

            InetSocketAddress remoteAddress = new InetSocketAddress(P2P_IP, P2P_PORT);
            SemuxChannelInitializer ci = new SemuxChannelInitializer(kernel, remoteAddress);
            client.connectAsync(remoteAddress, ci).sync();

            // waiting for the HELLO message to be sent
            Thread.sleep(1000);
            assertEquals(1, kernel.getChannelManager().getActivePeers().size());

            client.close();
            assertEquals(0, kernel.getChannelManager().getActivePeers().size());
        } finally {
            ps.stop();
            assertFalse(ps.getServer().isListening());
        }
    }
}
