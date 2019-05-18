/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public interface BlockEncoder {

    byte[] toBytes(Block block);

    byte[] getEncodedHeader(Block block);

    byte[] getEncodedTransactions(Block block);

    Pair<byte[], List<Integer>> getEncodedTransactionsAndIndices(Block block);

    byte[] getEncodedResults(Block block);

    Pair<byte[], List<Integer>> getEncodedResultsAndIndex(Block block);

    byte[] getEncodedVotes(Block block);
}
