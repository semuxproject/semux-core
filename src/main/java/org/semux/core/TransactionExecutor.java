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
import org.semux.core.TransactionResult.Error;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.util.Bytes;

/**
 * Transaction executor
 */
public class TransactionExecutor {

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
                result.setError(Error.INVALID_NONCE);
                continue;
            }

            // check transaction fee
            if (fee < config.minTransactionFee()) {
                result.setError(Error.INVALID_FEE);
                continue;
            }

            switch (tx.getType()) {
            case TRANSFER: {
                if (data.length > config.maxTransferDataSize()) {
                    result.setError(Error.INVALID_DATA_LENGTH);
                    break;
                }

                if (fee <= available && value <= available && value + fee <= available) {

                    as.adjustAvailable(from, -value - fee);
                    as.adjustAvailable(to, value);

                    result.setSuccess(true);
                } else {
                    result.setError(Error.INSUFFICIENT_AVAILABLE);
                }
                break;
            }
            case DELEGATE: {
                if (data.length > 16 || !Bytes.toString(data).matches("[_a-z0-9]{4,16}")) {
                    result.setError(Error.INVALID_DELEGATE_NAME);
                    break;
                }
                if (value < config.minDelegateFee()) {
                    result.setError(Error.INVALID_FEE);
                    break;
                }

                if (fee <= available && value <= available && value + fee <= available) {
                    if (Arrays.equals(from, to) && ds.register(to, data)) {

                        as.adjustAvailable(from, -value - fee);

                        result.setSuccess(true);
                    } else {
                        result.setError(Error.FAILED);
                    }
                } else {
                    result.setError(Error.INSUFFICIENT_AVAILABLE);
                }
                break;
            }
            case VOTE: {
                if (data.length > 0) {
                    result.setError(Error.INVALID_DATA_LENGTH);
                    break;
                }

                if (fee <= available && value <= available && value + fee <= available) {
                    if (ds.vote(from, to, value)) {

                        as.adjustAvailable(from, -value - fee);
                        as.adjustLocked(from, value);

                        result.setSuccess(true);
                    } else {
                        result.setError(Error.FAILED);
                    }
                } else {
                    result.setError(Error.INSUFFICIENT_AVAILABLE);
                }
                break;
            }
            case UNVOTE: {
                if (data.length > 0) {
                    result.setError(Error.INVALID_DATA_LENGTH);
                    break;
                }

                if (fee <= available && value <= locked) {
                    if (ds.unvote(from, to, value)) {

                        as.adjustAvailable(from, value - fee);
                        as.adjustLocked(from, -value);

                        result.setSuccess(true);
                    } else {
                        result.setError(Error.FAILED);
                    }
                } else {
                    result.setError(Error.INSUFFICIENT_LOCKED);
                }
                break;
            }
            default:
                // unsupported transaction type
                result.setError(Error.INVALID_TYPE);
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
