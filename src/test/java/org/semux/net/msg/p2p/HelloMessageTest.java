package org.semux.net.msg.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.net.Peer;

public class HelloMessageTest {

    @Test
    public void TestIsValid() {
        EdDSA key = new EdDSA();
        Peer peer = new Peer("127.0.0.1", 5161, Config.P2P_VERSION, Config.getClientId(false), key.toAddressString(),
                2);

        HelloMessage msg = new HelloMessage(peer, key);
        assertTrue(msg.isValid());
        assertEquals(key.toAddressString(), msg.getPeer().getPeerId());

        msg = new HelloMessage(msg.getEncoded());
        assertTrue(msg.isValid());
        assertEquals(key.toAddressString(), msg.getPeer().getPeerId());
    }
}
