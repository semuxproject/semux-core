package org.semux.consensus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.semux.core.Block;
import org.semux.core.Genesis;
import org.semux.crypto.EdDSA;

public class ProposalTest {

    @Test
    public void testProposal() {
        long height = Long.MAX_VALUE;
        int view = Integer.MAX_VALUE;
        Block block = Genesis.getInstance();
        Vote vote = Vote.newApprove(VoteType.COMMIT, height, view, block.getHash());
        vote.sign(new EdDSA());

        Proposal p = new Proposal(new Proof(height, view, Collections.singletonList(vote)), block);
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
}
