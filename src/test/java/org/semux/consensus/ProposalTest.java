package org.semux.consensus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hash;
import org.semux.util.MerkleUtil;

public class ProposalTest {

    @Test
    public void testProposal() {
        long height = Long.MAX_VALUE;
        int view = Integer.MAX_VALUE;
        Block block = createBlock(height);
        Vote vote = Vote.newReject(VoteType.VALIDATE, height, view - 1);
        vote.sign(new EdDSA());

        Proof proof = new Proof(height, view, Collections.singletonList(vote));
        Proposal p = new Proposal(proof, block.getHeader(), block.getTransactions());
        assertFalse(p.validate());
        p.sign(new EdDSA());
        assertTrue(p.validate());

        Proposal p2 = Proposal.fromBytes(p.toBytes());

        assertEquals(height, p2.getHeight());
        assertEquals(view, p2.getView());
        assertArrayEquals(block.getHash(), p2.getBlockHeader().getHash());
        assertEquals(1, p2.getProof().getVotes().size());
        assertArrayEquals(vote.getBlockHash(), p2.getProof().getVotes().get(0).getBlockHash());
    }

    private Block createBlock(long number) {
        List<Transaction> txs = Collections.emptyList();
        List<TransactionResult> res = Collections.emptyList();

        EdDSA key = new EdDSA();
        byte[] coinbase = key.toAddress();
        byte[] prevHash = Hash.EMPTY_H256;
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
        byte[] stateRoot = Hash.EMPTY_H256;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);
        return new Block(header.sign(key), txs, res);
    }
}
