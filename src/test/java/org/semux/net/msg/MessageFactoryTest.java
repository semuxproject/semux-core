/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MessageFactoryTest {

    @Test
    public void testNonExist() {
        MessageFactory mf = new MessageFactory();
        assertNull(mf.create((byte) 0xff, new byte[1]));
    }
}
