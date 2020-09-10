/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashTest {

    private static final Logger logger = LoggerFactory.getLogger(HashTest.class);

    private String msg = "test";
    private String msgBlake2b = "928b20366943e2afd11ebc0eae2e53a93bf177a4fcf35bcc64d503704e65e202";
    private String msgH160 = "86e8402b7615f07a2acb2ef1f4a54d323bbede77";

    @Test
    public void testEmptyHash() {
        byte[] x = new byte[0];
        byte[] hash = Hash.h256(x);

        logger.info("Hash of empty byte array = {}", Hex.encode(hash));
    }

    @Test
    public void testH256() {
        byte[] raw = Bytes.of(msg);
        byte[] hash = Hash.h256(raw);

        assertEquals(msgBlake2b, Hex.encode(hash));
    }

    @Test
    public void testH256Merge() {
        byte[] raw1 = Bytes.of(msg.substring(0, 1));
        byte[] raw2 = Bytes.of(msg.substring(1));
        byte[] hash = Hash.h256(raw1, raw2);

        assertEquals(msgBlake2b, Hex.encode(hash));
    }

    @Test
    public void testH160() {
        byte[] raw = Bytes.of(msg);
        byte[] hash = Hash.h160(raw);

        assertEquals(msgH160, Hex.encode(hash));
        assertEquals(20, hash.length);
    }
}
