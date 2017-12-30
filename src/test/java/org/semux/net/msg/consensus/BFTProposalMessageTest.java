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
import org.semux.consensus.Proof;
import org.semux.consensus.Proposal;
import org.semux.core.BlockHeader;
import org.semux.crypto.EdDSA;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

public class BFTProposalMessageTest {
    @Test
    public void testSerialization() {
        int height = 1;
        int view = 1;
        Proof proof = new Proof(height, view, Collections.emptyList());

        long number = 1;
        byte[] coinbase = Bytes.random(EdDSA.ADDRESS_LEN);
        byte[] prevHash = Bytes.random(32);
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(Collections.emptyList());
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(Collections.emptyList());
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};
        BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                stateRoot, data);

        Proposal proposal = new Proposal(proof, header, Collections.emptyList());
        proposal.sign(new EdDSA());

        BFTProposalMessage msg = new BFTProposalMessage(proposal);
        BFTProposalMessage msg2 = new BFTProposalMessage(msg.getEncoded());

        assertThat(msg2.getProposal()).isEqualToComparingFieldByFieldRecursively(proposal);
    }
}
