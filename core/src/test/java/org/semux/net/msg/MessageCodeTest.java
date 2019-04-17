/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MessageCodeTest {

    @Test
    public void testConverting() {
        for (MessageCode code : MessageCode.values()) {
            assertEquals(code, MessageCode.of(code.getCode()));
            assertEquals(code, MessageCode.of(code.toByte()));
        }
    }

    @Test
    public void testNegativeByte() {
        byte b = (byte) 0xff;
        assertNull(MessageCode.of(b));
    }
}
