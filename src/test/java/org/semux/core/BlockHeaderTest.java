/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.Config;
import org.semux.crypto.EdDSA;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockHeaderTest {
    private static Logger logger = LoggerFactory.getLogger(BlockHeaderTest.class);

    private long number = 1;
    private byte[] coinbase = Bytes.random(20);
    private byte[] prevHash = Bytes.random(32);
    private long timestamp = System.currentTimeMillis();
    private byte[] transactionsRoot = Bytes.random(32);
    private byte[] resultsRoot = Bytes.random(32);
    private byte[] stateRoot = Bytes.random(32);
    private byte[] data = Bytes.of("data");

    private EdDSA key = new EdDSA();
    private byte[] hash;
    private byte[] signature;

    @Test
    public void testNew() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data).sign(key);
        hash = header.getHash();
        signature = header.getSignature().toBytes();

        testFields(header);
    }

    @Test
    public void testSerilization() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data).sign(key);
        hash = header.getHash();
        signature = header.getSignature().toBytes();

        testFields(BlockHeader.fromBytes(header.toBytes()));
    }

    @Test
    public void testBlockHeaderSize() {
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data).sign(key);
        byte[] bytes = header.toBytes();

        logger.info("block header size: {}", bytes.length);
        logger.info("block header size (1y): {} GB",
                1.0 * bytes.length * Config.BLOCKS_PER_DAY * 365 / 1024 / 1024 / 1024);
    }

    private void testFields(BlockHeader header) {
        assertArrayEquals(hash, header.getHash());
        assertEquals(number, header.getNumber());
        assertArrayEquals(coinbase, header.getCoinbase());
        assertArrayEquals(prevHash, header.getPrevHash());
        assertEquals(timestamp, header.getTimestamp());
        assertArrayEquals(transactionsRoot, header.getTransactionsRoot());
        assertArrayEquals(resultsRoot, header.getResultsRoot());
        assertArrayEquals(stateRoot, header.getStateRoot());
        assertArrayEquals(data, header.getData());
        assertArrayEquals(signature, header.getSignature().toBytes());
    }
}
