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
     * @param commit
     *            whether to commit the updates
     * @return
     */
    public List<TransactionResult> execute(List<Transaction> txs, AccountState as, DelegateState ds, boolean commit) {
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

            // check nonce and balance
            if (nonce != fromAcc.getNonce() + 1 || fee > fromAcc.getBalance()) {
                continue;
            } else {
                // increase nonce
                fromAcc.setNonce(nonce);

                // charge transaction fee
                fromAcc.setBalance(fromAcc.getBalance() - fee);

                // set transaction to be valid
                result.setValid(true);
            }

            switch (tx.getType()) {
            case TRANSFER: {
                if (value <= fromAcc.getBalance()) {
                    // transfer balance
                    fromAcc.setBalance(fromAcc.getBalance() - value);
                    toAcc.setBalance(toAcc.getBalance() + value);

                    result.setCode(0);
                }
                break;
            }
            case DELEGATE: {
                if (Arrays.equals(from, to) //
                        && value == Config.BFT_REGISTRATION_FEE //
                        && value <= fromAcc.getBalance() //
                        && data.length <= 16 && Bytes.toString(data).matches("[_a-z]{4,16}")) {
                    // register delegate
                    fromAcc.setBalance(fromAcc.getBalance() - value);
                    ds.register(to, data);

                    result.setCode(0);
                }
                break;
            }
            case VOTE: {
                if (value <= fromAcc.getBalance() && ds.vote(from, to, value)) {
                    // lock balance
                    fromAcc.setBalance(fromAcc.getBalance() - value);
                    fromAcc.setLocked(fromAcc.getLocked() + value);

                    result.setCode(0);
                }
                break;
            }
            case UNVOTE: {
                if (value <= fromAcc.getLocked() && ds.unvote(from, to, value)) {
                    // unlock balance
                    fromAcc.setBalance(fromAcc.getBalance() + value);
                    fromAcc.setLocked(fromAcc.getLocked() - value);

                    result.setCode(0);
                }
                break;
            }
            default:
                logger.debug("Unsupported transaction type: {}", tx.getType());
                break;
            }
        }

        if (commit) {
            as.commit();
            ds.commit();
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
     * @param commit
     *            whether to commit the updates
     * @return
     */
    public TransactionResult execute(Transaction tx, AccountState as, DelegateState ds, boolean commit) {
        return execute(Collections.singletonList(tx), as, ds, commit).get(0);
    }
}
