/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Test;
import org.semux.core.BlockHeader;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

public class BlockHeaderMessageTest {

    @Test
    public void testSerialization() {
        long number = 1;
        byte[] coinbase = Bytes.random(Key.ADDRESS_LEN);
        byte[] prevHash = Bytes.random(32);
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(Collections.emptyList());
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(Collections.emptyList());
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);

        BlockHeaderMessage m = new BlockHeaderMessage(header);
        BlockHeaderMessage m2 = new BlockHeaderMessage(m.getEncoded());

        assertThat(m2.getHeader()).isEqualToComparingFieldByField(header);
    }
}
