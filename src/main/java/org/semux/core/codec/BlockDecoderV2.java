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
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateStateV2;
import org.semux.crypto.Key;
import org.semux.util.SimpleDecoder;

public class BlockDecoderV2 extends BlockDecoderV1 {

    private final DelegateStateV2 delegateState;

    BlockDecoderV2(DelegateStateV2 delegateState) {
        this.delegateState = delegateState;
    }

    @Override
    public Pair<Integer, List<Key.Signature>> decodeVotes(byte[] v) {
        List<Key.Signature> votes = new ArrayList<>();
        int view = 0;
        if (v != null) {
            SimpleDecoder dec = new SimpleDecoder(v);
            view = dec.readInt();
            int n = dec.readInt();
            for (int i = 0; i < n; i++) {
                int index = dec.readInt();
                byte[] signatureBytes = dec.readBytes();
                Delegate delegate = delegateState.getDelegateByIndex(index);
                votes.add(new Key.Signature(signatureBytes, delegate.getAbyte()));
            }
        }
        return Pair.of(view, votes);
    }
}
