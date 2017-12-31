/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.semux.net.msg.consensus.GetBlockHeaderMessage;

public class GetBlockHeaderMessageTest {

    @Test
    public void testSerialization() {
        long number = 1;

        GetBlockHeaderMessage m = new GetBlockHeaderMessage(number);
        GetBlockHeaderMessage m2 = new GetBlockHeaderMessage(m.getEncoded());

        assertThat(m2.getNumber()).isEqualTo(number);
    }
}
