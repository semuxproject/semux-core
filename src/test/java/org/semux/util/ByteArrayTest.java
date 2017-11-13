package org.semux.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;

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
}
