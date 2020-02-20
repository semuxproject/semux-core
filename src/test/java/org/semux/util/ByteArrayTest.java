/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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

    @Test
    public void testByteArrayKeyDeserializer() throws IOException {
        byte[] x = Bytes.random(3);
        ByteArray.ByteArrayKeyDeserializer d = new ByteArray.ByteArrayKeyDeserializer();
        Object y = d.deserializeKey(Hex.encode0x(x), null);
        assertTrue(y instanceof ByteArray);
        assertThat(ByteArray.of(x), equalTo(y));
    }
}
