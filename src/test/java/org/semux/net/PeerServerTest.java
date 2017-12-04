/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.Socket;

import org.junit.Test;
import org.semux.crypto.EdDSA;

public class PeerServerTest {

    private static final String P2P_IP = "127.0.0.1";
    private static final int P2P_PORT = 15161;

    @Test
    public void testServer() throws InterruptedException {
        EdDSA key = new EdDSA();
        PeerClient remoteClient = new PeerClient(P2P_IP, P2P_PORT, key);

        PeerServerMock ps = new PeerServerMock();
        ps.start(P2P_IP, P2P_PORT);
        assertTrue(ps.getServer().isListening());

        try (Socket sock = new Socket(remoteClient.getIp(), remoteClient.getPort())) {
            sock.getInputStream();
        } catch (IOException e) {
            fail(String.format("Server is not listenning at [%s:%s]", remoteClient.getIp(), remoteClient.getPort()));
        }

        ps.stop();
        assertFalse(ps.getServer().isListening());
    }
}
