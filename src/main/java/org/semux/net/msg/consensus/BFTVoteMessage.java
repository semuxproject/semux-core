/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.consensus.Vote;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;

public class BFTVoteMessage extends Message {

    private Vote vote;

    public BFTVoteMessage(Vote vote) {
        super(MessageCode.BFT_VOTE, null);
        this.vote = vote;

        this.encoded = vote.toBytes();
    }

    public BFTVoteMessage(byte[] encoded) {
        super(MessageCode.BFT_VOTE, null);
        this.encoded = encoded;

        this.vote = Vote.fromBytes(encoded);
    }

    public Vote getVote() {
        return vote;
    }

    @Override
    public String toString() {
        return "BFTVoteMessage: " + vote;
    }
}
