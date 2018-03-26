/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.util.Collections;
import java.util.List;

import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

public class TestUtils {

    public static Block createBlock(long number, List<Transaction> txs, List<TransactionResult> res) {
        Key key = new Key();
        byte[] coinbase = key.toAddress();
        byte[] prevHash = Bytes.EMPTY_HASH;
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        return new Block(header, txs, res);
    }

    public static Block createEmptyBlock(long number) {
        return createBlock(number, Collections.emptyList(), Collections.emptyList());
    }
}
