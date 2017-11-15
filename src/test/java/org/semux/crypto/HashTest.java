/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.util.Bytes;

public class HashTest {

    private String msg = "test";
    private String msgBlake2b = "928b20366943e2afd11ebc0eae2e53a93bf177a4fcf35bcc64d503704e65e202";

    @Test
    public void testH256() {
        byte[] raw = Bytes.of(msg);
        byte[] hash = Hash.h256(raw);

        assertEquals(msgBlake2b, Hex.encode(hash));
    }
}
