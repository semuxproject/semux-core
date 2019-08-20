/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.ethereum.vm.LogInfo;
import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.client.TransactionReceipt;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.semux.config.Config;
import org.semux.core.TransactionResult.Code;
import org.semux.core.state.Account;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.util.Bytes;
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxInternalTransaction;
import org.semux.vm.client.SemuxRepository;
import org.semux.vm.client.SemuxTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction executor
 */
public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionExecutor.class);

    private static final boolean[] valid = new boolean[256];

    static {
        for (byte b : Bytes.of("abcdefghijklmnopqrstuvwxyz0123456789_")) {
            valid[b & 0xff] = true;
        }
    }

    private BlockStore blockStore;

    /**
     * Validate delegate name.
     *
     * @param data
     */
    public static boolean validateDelegateName(byte[] data) {
        if (data.length < 3 || data.length > 16) {
            return false;
        }

        for (byte b : data) {
            if (!valid[b & 0xff]) {
                return false;
            }
        }

        return true;
    }

    private Config config;

    /**
     * Creates a new transaction executor.
     *
     * @param config
     */
    public TransactionExecutor(Config config, BlockStore blockStore) {
        this.config = config;
        this.blockStore = blockStore;
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
     * @param block
     *            the block context
     * @param gasUsedInBlock
     *            the amount of gas that has been consumed by previous transaction
     *            in the block
     * @return
     */
    public List<TransactionResult> execute(List<Transaction> txs, AccountState as, DelegateState ds,
            SemuxBlock block, Blockchain chain, long gasUsedInBlock) {
        List<TransactionResult> results = new ArrayList<>();

        for (Transaction tx : txs) {
            TransactionResult result = new TransactionResult();
            results.add(result);

            TransactionType type = tx.getType();
            byte[] from = tx.getFrom();
            byte[] to = tx.getTo();
            Amount value = tx.getValue();
            long nonce = tx.getNonce();
            Amount fee = tx.getFee();
            byte[] data = tx.getData();

            Account acc = as.getAccount(from);
            Amount available = acc.getAvailable();
            Amount locked = acc.getLocked();

            try {
                // check nonce
                if (nonce != acc.getNonce()) {
                    result.setCode(Code.INVALID_NONCE);
                    continue;
                }

                // check fee (CREATE and CALL use gas instead)
                if (tx.isVMTransaction()) {
                    // applying a very strict check to avoid mistakes
                    boolean valid = fee.equals(Amount.ZERO)
                            && tx.getGas() >= 21_000 && tx.getGas() <= config.spec().maxBlockGasLimit()
                            && tx.getGasPrice().greaterThanOrEqual(Amount.ONE)
                            && tx.getGasPrice().lessThanOrEqual(Amount.of(Integer.MAX_VALUE));
                    if (!valid) {
                        result.setCode(Code.INVALID_FEE);
                        continue;
                    }
                } else {
                    if (fee.lessThan(config.spec().minTransactionFee())) {
                        result.setCode(Code.INVALID_FEE);
                        continue;
                    }
                }

                // check data length
                if (data.length > config.spec().maxTransactionDataSize(type)) {
                    result.setCode(Code.INVALID_DATA);
                    continue;
                }

                // check remaining gas
                if (!tx.isVMTransaction()) {
                    if (config.spec().nonVMTransactionGasCost() + gasUsedInBlock > block.getGasLimit()) {
                        result.setCode(Code.INVALID);
                        continue;
                    }

                    // Note: although we count gas usage for non-vm-transactions, the gas usage
                    // is not recorded in the TransactionResult.
                }

                switch (type) {
                case TRANSFER: {
                    if (fee.lessThanOrEqual(available) && value.lessThanOrEqual(available)
                            && value.add(fee).lessThanOrEqual(available)) {
                        as.adjustAvailable(from, value.add(fee).negate());
                        as.adjustAvailable(to, value);
                    } else {
                        result.setCode(Code.INSUFFICIENT_AVAILABLE);
                    }
                    break;
                }
                case DELEGATE: {
                    if (!validateDelegateName(data)) {
                        result.setCode(Code.INVALID_DELEGATE_NAME);
                        break;
                    }
                    if (value.lessThan(config.spec().minDelegateBurnAmount())) {
                        result.setCode(Code.INVALID_DELEGATE_BURN_AMOUNT);
                        break;
                    }
                    if (!Arrays.equals(Bytes.EMPTY_ADDRESS, to)) {
                        result.setCode(Code.INVALID_DELEGATE_BURN_ADDRESS);
                        break;
                    }

                    if (fee.lessThanOrEqual(available) && value.lessThanOrEqual(available)
                            && value.add(fee).lessThanOrEqual(available)) {
                        if (ds.register(from, data)) {
                            as.adjustAvailable(from, value.add(fee).negate());
                        } else {
                            result.setCode(Code.INVALID_DELEGATING);
                        }
                    } else {
                        result.setCode(Code.INSUFFICIENT_AVAILABLE);
                    }
                    break;
                }
                case VOTE: {
                    if (fee.lessThanOrEqual(available) && value.lessThanOrEqual(available)
                            && value.add(fee).lessThanOrEqual(available)) {
                        if (ds.vote(from, to, value)) {
                            as.adjustAvailable(from, value.add(fee).negate());
                            as.adjustLocked(from, value);
                        } else {
                            result.setCode(Code.INVALID_VOTING);
                        }
                    } else {
                        result.setCode(Code.INSUFFICIENT_AVAILABLE);
                    }
                    break;
                }
                case UNVOTE: {
                    if (available.lessThan(fee)) {
                        result.setCode(Code.INSUFFICIENT_AVAILABLE);
                        break;
                    }
                    if (locked.lessThan(value)) {
                        result.setCode(Code.INSUFFICIENT_LOCKED);
                        break;
                    }

                    if (ds.unvote(from, to, value)) {
                        as.adjustAvailable(from, value.subtract(fee));
                        as.adjustLocked(from, value.negate());
                    } else {
                        result.setCode(Code.INVALID_UNVOTING);
                    }
                    break;
                }
                case CALL:
                case CREATE:
                    if (!chain.isForkActivated(Fork.VIRTUAL_MACHINE)) {
                        result.setCode(Code.INVALID_TYPE);
                        break;
                    }

                    // the VM transaction executor will check balance and gas cost.
                    // do proper refunds afterwards.
                    executeVmTransaction(result, tx, as, ds, block, gasUsedInBlock);

                    // Note: we're assuming the VM will not make changes to the account
                    // and delegate state if the transaction is INVALID; the storage changes
                    // will be discarded if is FAILURE.
                    //
                    // TODO: add unit test for this
                    break;
                default:
                    // unsupported transaction type
                    result.setCode(Code.INVALID_TYPE);
                    break;
                }
            } catch (ArithmeticException ae) {
                logger.warn("An arithmetic exception occurred during transaction execution: {}", tx);
                result.setCode(Code.INVALID);
            }

            if (result.getCode().isAcceptable()) {
                if (!tx.isVMTransaction()) {
                    // CREATEs and CALLs manages the nonce inside the VM
                    as.increaseNonce(from);
                }

                if (tx.isVMTransaction()) {
                    gasUsedInBlock += result.getGasUsed();
                } else {
                    gasUsedInBlock += config.spec().nonVMTransactionGasCost();
                }
            }

            result.setBlockNumber(block.getNumber());
        }

        return results;
    }

    private void executeVmTransaction(TransactionResult result, Transaction tx, AccountState as, DelegateState ds,
            SemuxBlock block, long gasUsedInBlock) {
        SemuxTransaction transaction = new SemuxTransaction(tx);
        Repository repository = new SemuxRepository(as, ds);
        ProgramInvokeFactory invokeFactory = new ProgramInvokeFactoryImpl();

        org.ethereum.vm.client.TransactionExecutor executor = new org.ethereum.vm.client.TransactionExecutor(
                transaction, block, repository, blockStore,
                config.spec().vmSpec(), invokeFactory, gasUsedInBlock, false);

        TransactionReceipt summary = executor.run();

        if (summary == null) {
            result.setCode(Code.INVALID);
        } else {
            result.setCode(summary.isSuccess() ? Code.SUCCESS : Code.FAILURE);
            result.setReturnData(summary.getReturnData());
            for (LogInfo log : summary.getLogs()) {
                result.addLog(log);
            }

            result.setGas(tx.getGas(), tx.getGasPrice(), summary.getGasUsed());

            result.setBlockNumber(block.getNumber());
            result.setInternalTransactions(summary.getInternalTransactions()
                    .stream()
                    .map(SemuxInternalTransaction::new)
                    .collect(Collectors.toList()));
        }
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
     * @param chain
     *            the blockchain instance
     * @param block
     *            the block context
     * @param gasUsedInBlock
     *            the amount of gas that has been consumed by previous transaction
     *            in the block
     * @return
     */
    public TransactionResult execute(Transaction tx, AccountState as, DelegateState ds, SemuxBlock block,
            Blockchain chain, long gasUsedInBlock) {
        return execute(Collections.singletonList(tx), as, ds, block, chain, gasUsedInBlock).get(0);
    }
}
