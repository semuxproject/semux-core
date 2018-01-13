/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg;

import org.junit.Test;

public class MessageFactoryTest {

    @Test(expected = MessageException.class)
    public void testNonExist() throws MessageException {
        MessageFactory factory = new MessageFactory();
        factory.create((byte) 0xff, new byte[1]);
    }
}
