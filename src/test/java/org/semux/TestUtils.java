/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

import java.util.Collections;
import java.util.List;

import org.semux.config.Config;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;

public class TestUtils {

    public static Block createBlock(long number, List<Transaction> txs, List<TransactionResult> res) {
        return createBlock(Bytes.EMPTY_HASH, new Key(), number, txs, res);
    }

    public static Block createBlock(byte[] prevHash, Key coinbase, long number, List<Transaction> txs,
            List<TransactionResult> res) {
        long timestamp = System.currentTimeMillis();
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
        byte[] stateRoot = Bytes.EMPTY_HASH;
        byte[] data = {};

        BlockHeader header = new BlockHeader(number, coinbase.toAddress(), prevHash, timestamp, transactionsRoot,
                resultsRoot,
                stateRoot, data);
        return new Block(header, txs, res);
    }

    public static Block createEmptyBlock(long number) {
        return createBlock(number, Collections.emptyList(), Collections.emptyList());
    }

    public static Transaction createTransaction(Config config) {
        return createTransaction(config, new Key(), new Key(), Amount.ZERO);
    }

    public static Transaction createTransaction(Config config, Key from, Key to, Amount value) {
        return createTransaction(config, from, to, value, 0);
    }

    public static Transaction createTransaction(Config config, Key from, Key to, Amount value, long nonce) {
        Network network = config.network();
        TransactionType type = TransactionType.TRANSFER;
        Amount fee = config.minTransactionFee();
        long timestamp = System.currentTimeMillis();
        byte[] data = {};

        return new Transaction(network, type, to.toAddress(), value, fee, nonce, timestamp, data).sign(from);
    }
}
