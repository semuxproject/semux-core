/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
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

    private TransactionType type = TransactionType.TRANSFER;
    private byte[] to = Bytes.random(20);
    private long value = 2;
    private long fee = config.minTransactionFee();
    private long nonce = 1;
    private long timestamp = System.currentTimeMillis();
    private byte[] data = Bytes.of("data");

    @Test
    public void testNew() {
        Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, data);
        assertNotNull(tx.getHash());
        assertNull(tx.getSignature());
        tx.sign(key);
        assertTrue(tx.validate());

        testFields(tx);
    }

    @Test
    public void testSerialization() {
        Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, data);
        tx.sign(key);

        testFields(Transaction.fromBytes(tx.toBytes()));
    }

    @Test
    public void testTransactionSize() {
        Transaction tx = new Transaction(type, to, value, fee, nonce, timestamp, Bytes.random(128)).sign(key);
        byte[] bytes = tx.toBytes();

        logger.info("tx size: {} B, {} GB per 1M txs", bytes.length, 1000000.0 * bytes.length / 1024 / 1024 / 1024);
    }

    @Test
    public void testTransactionSizeMaximumRecipients() {
        Transaction tx = new Transaction(
                TransactionType.TRANSFER_MANY,
                Bytes.random(EdDSA.ADDRESS_LEN * Transaction.MAX_RECIPIENTS), // 200 recipients
                value,
                fee,
                nonce,
                timestamp,
                Bytes.EMPTY_BYTES).sign(key);
        assertTrue(tx.validate());
        byte[] bytes = tx.toBytes();
        logger.info("tx size with maximum number of recipients: {} recipients, {} B, {} GB per 1M txs",
                Transaction.MAX_RECIPIENTS,
                bytes.length, 1000000.0 * bytes.length / 1024 / 1024 / 1024);
    }

    @Test
    public void testGetRecipients() {
        Transaction tx = new Transaction(TransactionType.TRANSFER_MANY, Bytes.random(EdDSA.ADDRESS_LEN * 10), value,
                fee, nonce, timestamp, Bytes.EMPTY_BYTES).sign(key);
        byte[][] recipients = tx.getRecipients();
        assertEquals(10, recipients.length);
        for (byte[] recipient : recipients) {
            assertEquals(EdDSA.ADDRESS_LEN, recipient.length);
        }
    }

    @Test
    public void testGetRecipient() {
        ArrayList<EdDSA> recipients = new ArrayList<>();
        int numberOfRecipients = RandomUtils.nextInt(1, Transaction.MAX_RECIPIENTS);
        for (int i = 0; i < numberOfRecipients; i++) {
            recipients.add(new EdDSA());
        }

        Transaction tx = new Transaction(
                TransactionType.TRANSFER_MANY,
                recipients.stream().map(EdDSA::toAddress).reduce(new byte[0], ArrayUtils::addAll),
                value,
                fee,
                nonce,
                timestamp,
                Bytes.EMPTY_BYTES);

        for (int i = 0; i < numberOfRecipients; i++) {
            assertArrayEquals(recipients.get(i).toAddress(), tx.getRecipient(i));
        }
    }

    @Test
    public void testValidateMultiRecipientException() {
        EnumSet<TransactionType> transactionTypes = EnumSet.allOf(TransactionType.class);
        transactionTypes.remove(TransactionType.TRANSFER_MANY);
        for (TransactionType type : transactionTypes) {
            Transaction tx = new Transaction(
                    type,
                    Bytes.random(EdDSA.ADDRESS_LEN * 2),
                    value,
                    fee,
                    nonce,
                    timestamp,
                    Bytes.EMPTY_BYTES);
            assertFalse(
                    "TRANSFER_MANY should be the only transaction type that supports multiple recipients",
                    tx.validate());
        }
    }

    @Test
    public void testValidateLargeTransaction() {
        Transaction tx = new Transaction(
                type,
                Bytes.random((Transaction.MAX_RECIPIENTS + 1) * EdDSA.ADDRESS_LEN),
                value,
                fee,
                nonce,
                timestamp,
                Bytes.EMPTY_BYTES);
        assertFalse(tx.validate());
    }

    private void testFields(Transaction tx) {
        assertEquals(type, tx.getType());
        assertArrayEquals(key.toAddress(), tx.getFrom());
        assertArrayEquals(to, tx.getRecipient(0));
        assertEquals(value, tx.getValue());
        assertEquals(fee, tx.getFee());
        assertEquals(nonce, tx.getNonce());
        assertEquals(timestamp, tx.getTimestamp());
        assertArrayEquals(data, tx.getData());
    }
}
