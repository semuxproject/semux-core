/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.crypto.Hash;
import org.semux.crypto.Key;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.TimeUtil;

public class VoteTest {

    @Test
    public void testVote() {
        long height = Long.MAX_VALUE;
        int view = Integer.MAX_VALUE;
        Vote vote = Vote.newApprove(VoteType.COMMIT, height, view, Bytes.EMPTY_HASH);

        assertFalse(vote.validate());
        vote.sign(new Key());
        assertTrue(vote.validate());

        Vote vote2 = Vote.fromBytes(vote.toBytes());

        assertEquals(VoteType.COMMIT, vote2.getType());
        assertEquals(height, vote2.getHeight());
        assertEquals(view, vote2.getView());
        assertArrayEquals(Bytes.EMPTY_HASH, vote2.getBlockHash());
    }

    @Test
    public void testValidate() {
        VoteType type = VoteType.COMMIT;

        long height = 1;
        int view = 0;
        byte[] blockHash = Bytes.EMPTY_HASH;

        Vote v = new Vote(type, false, height, view, blockHash);
        assertFalse(v.validate());
        v.sign(new Key());
        assertTrue(v.validate());
    }

    @Test
    public void testVotesSerialization() {
        Key key1 = new Key();
        Key key2 = new Key();

        List<Transaction> transactions = new ArrayList<>();
        List<TransactionResult> results = new ArrayList<>();

        long number = 1;
        byte[] coinbase = key1.toAddress();
        byte[] prevHash = Bytes.EMPTY_HASH;
        long timestamp = TimeUtil.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(transactions);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(results);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};
        int view = 1;

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        Block block = new Block(header, transactions, results);

        List<Key.Signature> votes = new ArrayList<>();
        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), view, block.getHash())
                .sign(key1);
        votes.add(vote.getSignature());
        vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), view, block.getHash()).sign(key2);
        votes.add(vote.getSignature());

        block.setView(view);
        block.setVotes(votes);
        block = Block.fromComponents(block.getEncodedHeader(), block.getEncodedTransactions(),
                block.getEncodedResults(),
                block.getEncodedVotes());

        for (Key.Signature sig : block.getVotes()) {
            ByteArray address = ByteArray.of(Hash.h160(sig.getPublicKey()));

            assertTrue(
                    address.equals(ByteArray.of(key1.toAddress())) || address.equals(ByteArray.of(key2.toAddress())));
            assertTrue(Key.verify(vote.getEncoded(), sig));
        }
    }
}
