/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.util.ArrayList;
import java.util.List;

import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.crypto.Hash;

public class MerkleUtil {

    private MerkleUtil() {
    }

    /**
     * Compute transactions merkle root.
     * 
     * @param txs
     *            transactions
     * @return
     */
    public static byte[] computeTransactionsRoot(List<Transaction> txs) {
        List<byte[]> hashes = new ArrayList<>();
        for (Transaction tx : txs) {
            hashes.add(tx.getHash());
        }
        return new MerkleTree(hashes).getRootHash();
    }

    /**
     * Compute results merkle root.
     * 
     * @param results
     *            transaction results
     * @return
     */
    public static byte[] computeResultsRoot(List<TransactionResult> results) {
        List<byte[]> hashes = new ArrayList<>();
        for (TransactionResult tx : results) {
            hashes.add(Hash.h256(tx.toBytes()));
        }
        return new MerkleTree(hashes).getRootHash();
    }
}
