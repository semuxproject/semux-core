/*
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
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.utils.Bytes;
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
     * NOTE: transaction format and signature are assumed to be valid.
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
            TransactionResult result = new TransactionResult();
            results.add(result);

            byte[] from = tx.getFrom();
            Account fromAcc = as.getAccount(from);
            byte[] to = tx.getTo();
            Account toAcc = as.getAccount(to);
            long value = tx.getValue();
            long nonce = tx.getNonce();
            long fee = tx.getFee();
            byte[] data = tx.getData();

            // check nonce
            if (nonce != fromAcc.getNonce()) {
                continue;
            }

            long balance = fromAcc.getAvailable();
            switch (tx.getType()) {
            case TRANSFER: {
                if (fee <= balance && value <= balance && value + fee <= balance) {
                    // transfer balance
                    fromAcc.setAvailable(fromAcc.getAvailable() - value - fee);
                    toAcc.setAvailable(toAcc.getAvailable() + value);

                    result.setValid(true);
                }
                break;
            }
            case DELEGATE: {
                if (fee <= balance && value <= balance && value + fee <= balance //
                        && Arrays.equals(from, to) //
                        && value >= Config.DELEGATE_BURN_AMOUNT //
                        && data.length <= 16 && Bytes.toString(data).matches("[_a-z0-9]{4,16}") //
                        && ds.register(to, data)) {
                    // register delegate
                    fromAcc.setAvailable(fromAcc.getAvailable() - value - fee);

                    result.setValid(true);
                }
                break;
            }
            case VOTE: {
                if (fee <= balance && value <= balance && value + fee <= balance //
                        && ds.vote(from, to, value)) {
                    // lock balance
                    fromAcc.setAvailable(fromAcc.getAvailable() - value - fee);
                    fromAcc.setLocked(fromAcc.getLocked() + value);

                    result.setValid(true);
                }
                break;
            }
            case UNVOTE: {
                if (fee <= balance //
                        && value <= fromAcc.getLocked() //
                        && ds.unvote(from, to, value)) {
                    // unlock balance
                    fromAcc.setAvailable(fromAcc.getAvailable() + value - fee);
                    fromAcc.setLocked(fromAcc.getLocked() - value);

                    result.setValid(true);
                }
                break;
            }
            default:
                logger.debug("Unsupported transaction type: {}", tx.getType());
                break;
            }

            // increase nonce if valid
            if (result.isValid()) {
                fromAcc.setNonce(nonce + 1);
            }
        }

        return results;
    }

    /**
     * Execute one transaction.
     * 
     * NOTE: transaction format and signature are assumed to be valid.
     * 
     * @param as
     *            account state
     * @param ds
     *            delegate state
     * @param txs
     *            transactions
     * @return
     */
    public TransactionResult execute(Transaction tx, AccountState as, DelegateState ds) {
        return execute(Collections.singletonList(tx), as, ds).get(0);
    }
}
