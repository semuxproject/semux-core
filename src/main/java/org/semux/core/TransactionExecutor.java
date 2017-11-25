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

import org.semux.Config;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction executor
 */
public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionExecutor.class);

    /**
     * Create a new transaction executor.
     */
    public TransactionExecutor() {
    }

    /**
     * Execute a list of transactions.
     * 
     * NOTE: transaction format and signature are assumed to be success.
     * 
     * @param as
     *            account state
     * @param ds
     *            delegate state
     * @param txs
     *            transactions
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
                continue;
            }

            switch (tx.getType()) {
            case TRANSFER: {
                if (fee <= available && value <= available && value + fee <= available) {

                    as.adjustAvailable(from, -value - fee);
                    as.adjustAvailable(to, value);

                    result.setSuccess(true);
                }
                break;
            }
            case DELEGATE: {
                if (fee <= available && value <= available && value + fee <= available //
                        && Arrays.equals(from, to) //
                        && value >= Config.DELEGATE_BURN_AMOUNT //
                        && data.length <= 16 && Bytes.toString(data).matches("[_a-z0-9]{4,16}") //
                        && ds.register(to, data)) {

                    as.adjustAvailable(from, -value - fee);

                    result.setSuccess(true);
                }
                break;
            }
            case VOTE: {
                if (fee <= available && value <= available && value + fee <= available //
                        && ds.vote(from, to, value)) {

                    as.adjustAvailable(from, -value - fee);
                    as.adjustLocked(from, value);

                    result.setSuccess(true);
                }
                break;
            }
            case UNVOTE: {
                if (fee <= available //
                        && value <= locked //
                        && ds.unvote(from, to, value)) {

                    as.adjustAvailable(from, value - fee);
                    as.adjustLocked(from, -value);

                    result.setSuccess(true);
                }
                break;
            }
            default:
                logger.debug("Unsupported transaction type: {}", tx.getType());
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
