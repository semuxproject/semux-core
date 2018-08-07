/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NewHeightMessageTest {
    @Test
    public void testSerialization() {
        int height = 1;

        NewHeightMessage msg = new NewHeightMessage(height);
        NewHeightMessage msg2 = new NewHeightMessage(msg.getBody());
        assertEquals(height, msg2.getHeight());
    }
}
