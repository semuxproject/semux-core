/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.semux.config.Config;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction executor
 */
public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionExecutor.class);

    private Config config;

    /**
     * Creates a new transaction executor.
     * 
     * @param config
     */
    public TransactionExecutor(Config config) {
        this.config = config;
    }

    /**
     * Execute a list of transactions.
     * 
     * NOTE: transaction format and signature are assumed to be success.
     *
     * @param txs
     *            transactions
     * @param as
     *            account state
     * @param ds
     *            delegate state
     * @return
     */
    public List<TransactionResult> execute(List<Transaction> txs, AccountState as, DelegateState ds) {
        List<TransactionResult> results = new ArrayList<>();

        for (Transaction tx : txs) {
            TransactionResult result = new TransactionResult(false);
            results.add(result);

            byte[] from = tx.getFrom();
            byte[] to = tx.getTo();
            long value = tx.getValue();
            long nonce = tx.getNonce();
            long fee = tx.getFee();
            byte[] data = tx.getData();

            Account acc = as.getAccount(from);
            long available = acc.getAvailable();
            long locked = acc.getLocked();

            // check nonce
            if (nonce != acc.getNonce()) {
                result.setError("Invalid nonce");
                continue;
            }

            // check transaction data
            if ((tx.getType() != TransactionType.TRANSFER && data.length != 0)
                    || (tx.getType() == TransactionType.TRANSFER && data.length > config.maxTransferDataSize())) {
                result.setError("Invalid data length");
                continue;
            }

            // check transaction fee
            if (fee < config.minTransactionFee()) {
                result.setError("Transaction fee too low");
                continue;
            }

            switch (tx.getType()) {
            case TRANSFER: {
                if (fee <= available && value <= available && value + fee <= available) {

                    as.adjustAvailable(from, -value - fee);
                    as.adjustAvailable(to, value);

                    result.setSuccess(true);
                } else {
                    result.setError("Insufficient available balance");
                }
                break;
            }
            case VOTE: {
                if (fee <= available && value <= available && value + fee <= available) {
                    if (ds.vote(from, to, value)) {
                        as.adjustAvailable(from, -value - fee);
                        as.adjustLocked(from, value);

                        result.setSuccess(true);
                    } else {
                        result.setError("Unable to vote");
                    }
                } else {
                    result.setError("Insufficient available balance");
                }
                break;
            }
            case UNVOTE: {
                if (fee <= available && value <= locked) {
                    if (ds.unvote(from, to, value)) {

                        as.adjustAvailable(from, value - fee);
                        as.adjustLocked(from, -value);

                        result.setSuccess(true);
                    } else {
                        result.setError("Unable to unvote");
                    }
                } else {
                    result.setError("Insufficient locked balance");
                }
                break;
            }
            default:
                result.setError("Unsupported type");
                break;
            }

            // increase nonce if success
            if (result.isSuccess()) {
                as.increaseNonce(from);
            }
        }

        return results;
    }

    /**
     * Execute one transaction.
     * 
     * NOTE: transaction format and signature are assumed to be success.
     * 
     * @param as
     *            account state
     * @param ds
     *            delegate state
     * @return
     */
    public TransactionResult execute(Transaction tx, AccountState as, DelegateState ds) {
        return execute(Collections.singletonList(tx), as, ds).get(0);
    }
}
