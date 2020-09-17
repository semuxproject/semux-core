/**
 * Copyright (c) 2017-2020 The Semux Developers
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
import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.UnitTestnetConfig;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTest {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTest.class);

    private Config config = new UnitTestnetConfig(Constants.DEFAULT_DATA_DIR);
    private Key key = new Key();

    private Network network = Network.DEVNET;
    private TransactionType type = TransactionType.TRANSFER;
    private byte[] to = Hex.decode0x("0xdb7cadb25fdcdd546fb0268524107582c3f8999c");
    private Amount value = Amount.of(2);
    private Amount fee = config.spec().minTransactionFee();
    private long nonce = 1;
    private long timestamp = 1523028482000L;
    private byte[] data = Bytes.of("data");

    private final byte[] encodedBytes = Hex.decode0x(
            "0x020114db7cadb25fdcdd546fb0268524107582c3f8999c000000000000000200000000004c4b400000000000000001000001629b9257d00464617461");

    @Test
    public void testNew() {
        Transaction tx = new Transaction(network, type, to, key.toAddress(), value, fee, nonce, timestamp, data, config.forkEd25519ContractEnabled());
        assertNotNull(tx.getHash());
        assertNull(tx.getSignature());
        tx.sign(key);
        assertTrue(tx.validate_verify_sign(network, config.forkEd25519ContractEnabled()));

        testFields(tx);
    }

    /**
     * Test serialization of a signed tx.
     */
    @Test
    public void testSerialization() {
        Transaction tx = new Transaction(network, type, to, key.toAddress(), value, fee, nonce, timestamp, data, config.forkEd25519ContractEnabled());
        tx.sign(key);

        testFields(Transaction.fromBytes(tx.toBytes()));
    }

    @Test
    public void testTransactionSize() {
        Transaction tx = new Transaction(network, type, to, key.toAddress(), value, fee, nonce, timestamp, Bytes.random(128), config.forkEd25519ContractEnabled())
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
        Transaction tx = new Transaction(network, type, to, key.toAddress(), value, fee, nonce, timestamp, Bytes.random(128), config.forkEd25519ContractEnabled())
                .sign(key);
        Transaction tx2 = new Transaction(network, type, to, key.toAddress(), value, fee, nonce, timestamp, tx.getData(), config.forkEd25519ContractEnabled())
                .sign(key);

        assertEquals(tx, tx2);
        assertEquals(tx.hashCode(), tx2.hashCode());
    }

    /**
     * Test encoding of an unsigned tx.
     */
    @Test
    public void testEncoding() {
        Transaction tx = new Transaction(
                network,
                type,
                to,
                key.toAddress(),
                value,
                fee,
                nonce,
                timestamp,
                data,
                config.forkEd25519ContractEnabled());

        assertArrayEquals(encodedBytes, tx.getEncoded());
    }

    /**
     * Test decoding of an unsigned tx.
     */
    @Test
    public void testDecoding() {
        Transaction tx = Transaction.fromEncoded(encodedBytes, Bytes.random(20), config.forkEd25519ContractEnabled());

        assertEquals(network.id(), tx.getNetworkId());
        assertEquals(type, tx.getType());
        assertArrayEquals(to, tx.getTo());
        assertEquals(value, tx.getValue());
        assertEquals(fee, tx.getFee());
        assertEquals(nonce, tx.getNonce());
        assertEquals(timestamp, tx.getTimestamp());
        assertArrayEquals(data, tx.getData());
    }
}
