package org.semux.consensus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.crypto.EdDSA;
import org.semux.crypto.Hash;

public class ProposalTest {

    @Test
    public void testProposal() {
        long height = Long.MAX_VALUE;
        int view = Integer.MAX_VALUE;
        Block block = createBlock(height);
        Vote vote = Vote.newReject(VoteType.VALIDATE, height, view - 1);
        vote.sign(new EdDSA());

        Proof proof = new Proof(height, view, Collections.singletonList(vote));
        Proposal p = new Proposal(proof, block);
        assertFalse(p.validate());
        p.sign(new EdDSA());
        assertTrue(p.validate());

        Proposal p2 = Proposal.fromBytes(p.toBytes());

        assertEquals(height, p2.getHeight());
        assertEquals(view, p2.getView());
        assertArrayEquals(block.getHash(), p2.getBlock().getHash());
        assertEquals(1, p2.getProof().getVotes().size());
        assertArrayEquals(vote.getBlockHash(), p2.getProof().getVotes().get(0).getBlockHash());
    }

    private Block createBlock(long number) {
        EdDSA key = new EdDSA();
        byte[] coinbase = key.toAddress();
        byte[] prevHash = Hash.EMPTY_H256;
        long timestamp = System.currentTimeMillis();
        byte[] merkleRoot = {};
        byte[] data = {};
        merkleRoot = Hash.EMPTY_H256;

        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, merkleRoot, data);
        return new Block(header.sign(key), Collections.emptyList());
    }
}
