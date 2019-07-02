/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.codec;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.core.Block;

public interface BlockEncoder {

    byte[] encode(Block block);

    byte[] encoderHeader(Block block);

    byte[] encodeTransactions(Block block);

    Pair<byte[], List<Integer>> getEncodedTransactionsAndIndices(Block block);

    byte[] encodeTransactionResults(Block block);

    Pair<byte[], List<Integer>> getEncodedResultsAndIndex(Block block);

    byte[] encodeVotes(Block block);
}
