/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BFTNewHeightMessageTest {
    @Test
    public void testSerialization() {
        int height = 1;

        BFTNewHeightMessage msg = new BFTNewHeightMessage(height);
        BFTNewHeightMessage msg2 = new BFTNewHeightMessage(msg.getEncoded());
        assertEquals(height, msg2.getHeight());
    }
}
