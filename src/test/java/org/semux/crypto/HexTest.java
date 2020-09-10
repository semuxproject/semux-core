/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HexTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    @Test
    public void testDecodeError() {
        expectedException.expect(CryptoException.class);
        Hex.decode("I_am_not_a_hexadecimal_string");
    }

    @Test
    public void testEncode() {
        String message = "Hello World";
        assertEquals("48656c6c6f20576f726c64", Hex.encode(message.getBytes()));
    }

    @Test
    public void testEncode0x() {
        String message = "Hello World";
        assertEquals("0x48656c6c6f20576f726c64", Hex.encode0x(message.getBytes()));
    }

    @Test
    public void testParse() {
        String encoded = "48656c6c6f20576f726c64";
        assertEquals("Hello World", new String(Hex.decode0x(encoded)));
    }

    @Test
    public void testParse0x() {
        String encoded = "0x48656c6c6f20576f726c64";
        assertEquals("Hello World", new String(Hex.decode0x(encoded)));
    }
}
