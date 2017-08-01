/*
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
import org.semux.crypto.EdDSA;
import org.semux.utils.Bytes;

public class TransactionTest {

    private EdDSA key = new EdDSA();

    private TransactionType type = TransactionType.TRANSFER;
    private byte[] from = Bytes.random(20);
    private byte[] to = Bytes.random(20);
    private long value = 2;
    private long fee = 1;
    private long nonce = 1;
    private long timestamp = System.currentTimeMillis();
    private byte[] data = Bytes.of("data");

    @Test
    public void testNew() {
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        assertNotNull(tx.getHash());
        assertNull(tx.getSignature());
        tx.sign(key);
        assertTrue(tx.validate());

        testFields(tx);
    }

    @Test
    public void testSerilization() {
        Transaction tx = new Transaction(type, from, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        testFields(Transaction.fromBytes(tx.toBytes()));
    }

    private void testFields(Transaction tx) {
        assertEquals(type, tx.getType());
        assertArrayEquals(from, tx.getFrom());
        assertArrayEquals(to, tx.getTo());
        assertEquals(value, tx.getValue());
        assertEquals(fee, tx.getFee());
        assertEquals(nonce, tx.getNonce());
        assertEquals(timestamp, tx.getTimestamp());
        assertArrayEquals(data, tx.getData());
    }
}
