/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.semux.crypto.Hex;

public class ByteArrayTest {

    @Test
    public void testInHashMap() {
        byte[] b1 = Bytes.random(20);
        byte[] b2 = Bytes.random(20);
        byte[] b3 = Arrays.copyOf(b1, b1.length);

        HashMap<ByteArray, Boolean> map = new HashMap<>();
        map.put(ByteArray.of(b1), true);

        assertFalse(map.containsKey(ByteArray.of(b2)));
        assertTrue(map.containsKey(ByteArray.of(b3)));
    }

    @Test
    public void testToString() {
        byte[] b = Bytes.random(20);
        assertEquals(Hex.encode(b), ByteArray.of(b).toString());
    }
}
