/**
 * Copyright (c) 2017 The Semux Developers
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

import org.junit.Test;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.DevNetConfig;
import org.semux.crypto.EdDSA;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTest {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTest.class);

    private Config config = new DevNetConfig(Constants.DEFAULT_DATA_DIR);
    private EdDSA key = new EdDSA();

    private byte networkId = Constants.DEV_NET_ID;
    private TransactionType type = TransactionType.TRANSFER;
    private byte[] to = Bytes.random(20);
    private long value = 2;
    private long fee = config.minTransactionFee();
    private long nonce = 1;
    private long timestamp = System.currentTimeMillis();
    private byte[] data = Bytes.of("data");

    @Test
    public void testNew() {
        Transaction tx = new Transaction(networkId, type, to, value, fee, nonce, timestamp, data);
        assertNotNull(tx.getHash());
        assertNull(tx.getSignature());
        tx.sign(key);
        assertTrue(tx.validate(networkId));

        testFields(tx);
    }

    @Test
    public void testSerialization() {
        Transaction tx = new Transaction(networkId, type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        testFields(Transaction.fromBytes(tx.toBytes()));
    }

    @Test
    public void testTransactionSize() {
        Transaction tx = new Transaction(networkId, type, to, value, fee, nonce, timestamp, Bytes.random(128))
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
}
