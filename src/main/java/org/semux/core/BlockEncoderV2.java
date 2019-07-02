/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.List;

import org.semux.core.state.DelegateStateV2;
import org.semux.crypto.Key;
import org.semux.util.SimpleEncoder;

public class BlockEncoderV2 extends BlockEncoderV1 {

    private final DelegateStateV2 delegateState;

    public BlockEncoderV2(DelegateStateV2 delegateState) {
        this.delegateState = delegateState;
    }

    @Override
    public byte[] encodeVotes(Block block) {
        List<Key.Signature> votes = block.getVotes();
        SimpleEncoder enc = new SimpleEncoder();

        enc.writeInt(block.getView());
        enc.writeInt(votes.size());
        for (Key.Signature vote : votes) {
            enc.writeInt(delegateState.getDelegateIndex(vote.getAddress()));
            enc.writeBytes(vote.getS());
        }

        return enc.toBytes();
    }
}
