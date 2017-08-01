/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HexTest {

    @Test
    public void testToHex() {
        byte[] raw = { 0x0f, (byte) 0xf0, 0x52, 0x25 };
        String hex = Hex.encode(raw);

        assertEquals("0ff05225", hex);
    }

    @Test
    public void testToBytes() {
        String hex = "0Ff05225";
        byte[] raw = Hex.decode(hex);

        assertArrayEquals(new byte[] { 0x0f, (byte) 0xf0, 0x52, 0x25 }, raw);
    }
}
