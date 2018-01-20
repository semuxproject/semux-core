/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.p2p;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.semux.Network;
import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.crypto.Key;
import org.semux.util.Bytes;

public class TransactionMessageTest {
    @Test
    public void testSerialization() {
        Network network = Network.DEVNET;
        TransactionType type = TransactionType.TRANSFER;
        byte[] to = Bytes.random(20);
        long value = 2;
        long fee = 50_000_000L;
        long nonce = 1;
        long timestamp = System.currentTimeMillis();
        byte[] data = Bytes.of("data");

        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data);
        tx.sign(new Key());

        TransactionMessage msg = new TransactionMessage(tx);
        TransactionMessage msg2 = new TransactionMessage(msg.getEncoded());
        assertThat(msg2.getTransaction().getHash(), equalTo(tx.getHash()));
    }
}
