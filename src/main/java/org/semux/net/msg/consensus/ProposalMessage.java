/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.consensus.Proposal;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;

public class ProposalMessage extends Message {

    private Proposal proposal;

    public ProposalMessage(Proposal proposal) {
        super(MessageCode.BFT_PROPOSAL, null);
        this.proposal = proposal;

        this.encoded = proposal.toBytes();
    }

    public ProposalMessage(byte[] encoded) {
        super(MessageCode.BFT_PROPOSAL, null);
        this.encoded = encoded;

        this.proposal = Proposal.fromBytes(encoded);
    }

    public Proposal getProposal() {
        return proposal;
    }

    @Override
    public String toString() {
        return "BFTProposalMessage: " + proposal;
    }
}
