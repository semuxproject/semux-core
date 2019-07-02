/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.codec;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.core.Block;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.crypto.Key;
import org.semux.util.SimpleEncoder;

public class BlockEncoderV1 implements BlockEncoder {

    @Override
    public byte[] encode(Block block) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(encoderHeader(block));
        enc.writeBytes(encodeTransactions(block));
        enc.writeBytes(encodeTransactionResults(block));
        enc.writeBytes(encodeVotes(block));

        return enc.toBytes();
    }

    @Override
    public byte[] encoderHeader(Block block) {
        return block.getHeader().toBytes();
    }

    @Override
    public byte[] encodeTransactions(Block block) {
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
    public byte[] encodeTransactionResults(Block block) {
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
    public byte[] encodeVotes(Block block) {
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
