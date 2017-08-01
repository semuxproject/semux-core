/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.utils.Bytes;

public class BlockHeaderTest {

    private byte[] hash = Bytes.random(32);
    private long number = 1;
    private byte[] coinbase = Bytes.random(20);
    private byte[] prevHash = Bytes.random(32);
    private long timestamp = System.currentTimeMillis();
    private byte[] merkleRoot = Bytes.random(32);
    private byte[] data = Bytes.of("data");

    @Test
    public void testNew() {
        BlockHeader header = new BlockHeader(hash, number, coinbase, prevHash, timestamp, merkleRoot, data);

        testFields(header);
    }

    @Test
    public void testSerilization() {
        BlockHeader header = new BlockHeader(hash, number, coinbase, prevHash, timestamp, merkleRoot, data);

        testFields(BlockHeader.fromBytes(header.toBytes()));
    }

    private void testFields(BlockHeader header) {
        assertArrayEquals(hash, header.getHash());
        assertEquals(number, header.getNumber());
        assertArrayEquals(coinbase, header.getCoinbase());
        assertArrayEquals(prevHash, header.getPrevHash());
        assertEquals(timestamp, header.getTimestamp());
        assertArrayEquals(merkleRoot, header.getMerkleRoot());
        assertArrayEquals(data, header.getData());
        assertArrayEquals(hash, header.getHash());
    }
}
