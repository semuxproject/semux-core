/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.crypto.Key;
import org.semux.util.SimpleEncoder;

public class BlockEncoderV1 implements BlockEncoder {

    @Override
    public byte[] toBytes(Block block) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(getEncodedHeader(block));
        enc.writeBytes(getEncodedTransactions(block));
        enc.writeBytes(getEncodedResults(block));
        enc.writeBytes(getEncodedVotes(block));

        return enc.toBytes();
    }

    @Override
    public byte[] getEncodedHeader(Block block) {
        return block.getHeader().toBytes();
    }

    @Override
    public byte[] getEncodedTransactions(Block block) {
        return getEncodedTransactionsAndIndices(block).getLeft();
    }

    @Override
    public Pair<byte[], List<Integer>> getEncodedTransactionsAndIndices(Block block) {
        List<Integer> indices = new ArrayList<>();
        List<Transaction> transactions = block.getTransactions();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(transactions.size());
        for (int i = 0; i < transactions.size(); i++) {
            int index = enc.getWriteIndex();
            enc.writeBytes(transactions.get(i).toBytes());
            indices.add(index);
        }

        return Pair.of(enc.toBytes(), indices);
    }

    @Override
    public byte[] getEncodedResults(Block block) {
        return getEncodedResultsAndIndex(block).getLeft();
    }

    @Override
    public Pair<byte[], List<Integer>> getEncodedResultsAndIndex(Block block) {
        List<Integer> indices = new ArrayList<>();
        List<TransactionResult> results = block.getResults();

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(results.size());
        for (int i = 0; i < results.size(); i++) {
            int index = enc.getWriteIndex();
            enc.writeBytes(results.get(i).toBytes());
            indices.add(index);
        }

        return Pair.of(enc.toBytes(), indices);
    }

    @Override
    public byte[] getEncodedVotes(Block block) {
        List<Key.Signature> votes = block.getVotes();
        SimpleEncoder enc = new SimpleEncoder(4 + 4 + votes.size() * Key.Signature.LENGTH);

        enc.writeInt(block.getView());
        enc.writeInt(votes.size());
        for (Key.Signature vote : votes) {
            enc.writeBytes(vote.toBytes());
        }

        return enc.toBytes();
    }
}
