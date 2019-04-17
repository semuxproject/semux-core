/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.semux.net.msg.MessageCode;

public class GetBlockHeaderMessageTest {

    @Test
    public void testSerialization() {
        long number = 1;

        GetBlockHeaderMessage m = new GetBlockHeaderMessage(number);
        assertThat(m.getCode()).isEqualTo(MessageCode.GET_BLOCK_HEADER);
        assertThat(m.getResponseMessageClass()).isEqualTo(BlockHeaderMessage.class);

        GetBlockHeaderMessage m2 = new GetBlockHeaderMessage(m.getBody());
        assertThat(m2.getCode()).isEqualTo(MessageCode.GET_BLOCK_HEADER);
        assertThat(m2.getResponseMessageClass()).isEqualTo(BlockHeaderMessage.class);
        assertThat(m2.getNumber()).isEqualTo(number);
    }
}
