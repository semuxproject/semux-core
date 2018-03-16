/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.semux.core.Amount.Unit.NANO_SEM;

import org.junit.Test;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevnetConfig;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTest {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTest.class);

    private Config config = new DevnetConfig(Constants.DEFAULT_DATA_DIR);
    private Key key = new Key();

    private Network network = Network.DEVNET;
    private TransactionType type = TransactionType.TRANSFER;
    private byte[] to = Bytes.random(20);
    private Amount value = NANO_SEM.of(2);
    private Amount fee = config.minTransactionFee();
    private long nonce = 1;
    private long timestamp = System.currentTimeMillis();
    private byte[] data = Bytes.of("data");

    @Test
    public void testNew() {
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data);
        assertNotNull(tx.getHash());
        assertNull(tx.getSignature());
        tx.sign(key);
        assertTrue(tx.validate(network));

        testFields(tx);
    }

    @Test
    public void testSerialization() {
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        testFields(Transaction.fromBytes(tx.toBytes()));
    }

    @Test
    public void testTransactionSize() {
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, Bytes.random(128))
                .sign(key);
        byte[] bytes = tx.toBytes();

        logger.info("tx size: {} B, {} GB per 1M txs", bytes.length, 1000000.0 * bytes.length / 1024 / 1024 / 1024);
    }

    private void testFields(Transaction tx) {
        assertEquals(type, tx.getType());
        assertArrayEquals(key.toAddress(), tx.getFrom());
        assertArrayEquals(to, tx.getTo());
        assertEquals(value, tx.getValue());
        assertEquals(fee, tx.getFee());
        assertEquals(nonce, tx.getNonce());
        assertEquals(timestamp, tx.getTimestamp());
        assertArrayEquals(data, tx.getData());
    }

    @Test
    public void testEquality() {
        Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, Bytes.random(128))
                .sign(key);
        Transaction tx2 = new Transaction(network, type, to, value, fee, nonce, timestamp, tx.getData())
                .sign(key);

        assertEquals(tx, tx2);
        assertEquals(tx.hashCode(), tx2.hashCode());
    }
}
