/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.List;

import org.semux.crypto.Key;
import org.semux.util.SimpleDecoder;

public class BlockDecoderV1 implements BlockDecoder {

    /**
     * Parses a block instance from bytes.
     *
     * @param h
     *            Serialized header
     * @param t
     *            Serialized transactions
     * @param r
     *            Serialized transaction results
     * @param v
     *            Serialized votes
     * @return
     */
    @Override
    public Block fromComponents(byte[] h, byte[] t, byte[] r, byte[] v) {
        if (h == null) {
            throw new IllegalArgumentException("Block header can't be null");
        }
        if (t == null) {
            throw new IllegalArgumentException("Block transactions can't be null");
        }

        BlockHeader header = BlockHeader.fromBytes(h);

        SimpleDecoder dec = new SimpleDecoder(t);
        List<Transaction> transactions = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            transactions.add(Transaction.fromBytes(dec.readBytes()));
        }

        List<TransactionResult> results = new ArrayList<>();
        if (r != null) {
            dec = new SimpleDecoder(r);
            n = dec.readInt();
            for (int i = 0; i < n; i++) {
                results.add(TransactionResult.fromBytes(dec.readBytes()));
            }
        }

        int view = 0;
        List<Key.Signature> votes = new ArrayList<>();
        if (v != null) {
            dec = new SimpleDecoder(v);
            view = dec.readInt();
            n = dec.readInt();
            for (int i = 0; i < n; i++) {
                votes.add(Key.Signature.fromBytes(dec.readBytes()));
            }
        }

        return new Block(header, transactions, results, view, votes);
    }

    @Override
    public Block fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] header = dec.readBytes();
        byte[] transactions = dec.readBytes();
        byte[] results = dec.readBytes();
        byte[] votes = dec.readBytes();

        return fromComponents(header, transactions, results, votes);
    }
}
