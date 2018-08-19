/*
 * Copyright (c) [2018] [ The Semux Developers ]
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm.client;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.ethereum.vm.util.BigIntUtil.isCovers;
import static org.ethereum.vm.util.BigIntUtil.toBI;
import static org.ethereum.vm.util.BigIntUtil.transfer;
import static org.ethereum.vm.util.HexUtil.toHexString;

import java.math.BigInteger;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.VM;
import org.ethereum.vm.config.Config;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.exception.ExceptionFactory;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.ethereum.vm.util.ByteArrayWrapper;
import org.ethereum.vm.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionExecutor.class);

    private final Transaction tx;
    private final Block block;
    private long basicTxCost;

    private final Repository repo;
    private final Repository track;
    private final BlockStore blockStore;

    private final Config config;
    private final ProgramInvokeFactory invokeFactory;
    private final long gasUsedInTheBlock;
    private final boolean localCall;

    private boolean readyToExecute = false;
    private VM vm;
    private Program program;

    private ProgramResult result = new ProgramResult();
    private BigInteger gasLeft;

    public TransactionExecutor(Transaction tx, Block block, Repository repo, BlockStore blockStore, boolean localCall) {
        this(tx, block, repo, blockStore, Config.DEFAULT, new ProgramInvokeFactoryImpl(), 0, localCall);
    }

    public TransactionExecutor(Transaction tx, Block block, Repository repo, BlockStore blockStore,
            Config config, ProgramInvokeFactory invokeFactory, long gasUsedInTheBlock, boolean localCall) {
        this.tx = tx;
        this.block = block;
        this.basicTxCost = config.getTransactionCost(tx);

        this.repo = repo;
        this.track = repo.startTracking();
        this.blockStore = blockStore;

        this.config = config;
        this.invokeFactory = invokeFactory;
        this.gasUsedInTheBlock = gasUsedInTheBlock;
        this.localCall = localCall;

        this.gasLeft = tx.getGas();
    }

    /**
     * Do basic validation, e.g. nonce, balance and gas check, and prepare this
     * executor.
     *
     * The <code>ReadyToExecute</code> flag will be set to true on success.
     */
    public void init() {
        if (localCall) {
            readyToExecute = true;
            return;
        }

        BigInteger txGas = tx.getGas();
        BigInteger blockGasLimit = block.getGasLimit();

        if (txGas.add(BigInteger.valueOf(gasUsedInTheBlock)).compareTo(blockGasLimit) > 0) {
            logger.warn("Too much gas used in this block");
            return;
        }

        if (txGas.compareTo(BigInteger.valueOf(basicTxCost)) < 0) {
            logger.warn("Not enough gas to cover basic transaction cost: required = {}, actual = {}", basicTxCost,
                    txGas);
            return;
        }

        long reqNonce = repo.getNonce(tx.getFrom());
        long txNonce = tx.getNonce();
        if (reqNonce != txNonce) {
            logger.warn("Invalid nonce: required = {}, actual = {}", reqNonce, txNonce);
            return;
        }

        BigInteger txGasCost = tx.getGasPrice().multiply(txGas);
        BigInteger totalCost = tx.getValue().add(txGasCost);
        BigInteger senderBalance = repo.getBalance(tx.getFrom());
        if (!isCovers(senderBalance, totalCost)) {
            logger.warn("Not enough balance: required = {}, sender.balance = {}", totalCost, senderBalance);
            return;
        }

        readyToExecute = true;
    }

    /**
     * Executes the transaction.
     */
    public void execute() {
        if (!readyToExecute) {
            return;
        }

        if (!localCall) {
            // increase nonce
            repo.increaseNonce(tx.getFrom());

            // charge gas cost
            BigInteger txGasLimit = tx.getGas();
            BigInteger txGasCost = tx.getGasPrice().multiply(txGasLimit);
            repo.addBalance(tx.getFrom(), txGasCost.negate());
        }

        if (tx.isCreate()) {
            create();
        } else {
            call();
        }
    }

    protected void call() {
        if (!readyToExecute) {
            return;
        }

        byte[] targetAddress = tx.getTo();
        PrecompiledContracts.PrecompiledContract precompiledContract = PrecompiledContracts
                .getContractForAddress(new DataWord(targetAddress), config);

        if (precompiledContract != null) {
            long requiredGas = precompiledContract.getGasForData(tx.getData());

            BigInteger spendingGas = BigInteger.valueOf(requiredGas).add(BigInteger.valueOf(basicTxCost));
            if (!localCall && gasLeft.compareTo(spendingGas) < 0) {
                // no refund
                // no endowment
                logger.warn("Out of Gas calling precompiled contract: required {}, gasLeft = {}", spendingGas, gasLeft);
                gasLeft = BigInteger.ZERO;
                return;
            } else {
                gasLeft = gasLeft.subtract(spendingGas);
                Pair<Boolean, byte[]> out = precompiledContract.execute(tx.getData());

                if (!out.getLeft()) {
                    logger.warn("Error executing precompiled contract 0x{}", toHexString(targetAddress));
                    gasLeft = BigInteger.ZERO;
                    return;
                }
            }
        } else {
            byte[] code = repo.getCode(targetAddress);
            if (isEmpty(code)) {
                gasLeft = gasLeft.subtract(BigInteger.valueOf(basicTxCost));
                result.spendGas(basicTxCost);
            } else {
                ProgramInvoke programInvoke = invokeFactory.createProgramInvoke(tx, block, track, blockStore);

                this.vm = new VM(config);
                this.program = new Program(code, programInvoke, tx, config);
            }
        }

        BigInteger endowment = tx.getValue();
        transfer(track, tx.getFrom(), targetAddress, endowment);
    }

    protected void create() {
        byte[] newContractAddress = HashUtil.calcNewAddress(tx.getFrom(), tx.getNonce());

        if (track.isExist(newContractAddress)) {
            logger.warn("Trying to create a contract with existing contract address: 0x{}",
                    toHexString(newContractAddress));
            gasLeft = BigInteger.ZERO;
            return;
        }

        // In case of hashing collisions
        BigInteger oldBalance = repo.getBalance(newContractAddress);
        track.addBalance(newContractAddress, oldBalance);
        track.increaseNonce(newContractAddress);

        if (isEmpty(tx.getData())) {
            gasLeft = gasLeft.subtract(BigInteger.valueOf(basicTxCost));
            result.spendGas(basicTxCost);
        } else {
            ProgramInvoke programInvoke = invokeFactory.createProgramInvoke(tx, block, track, blockStore);

            this.vm = new VM(config);
            this.program = new Program(tx.getData(), programInvoke, tx, config);
        }

        BigInteger endowment = tx.getValue();
        transfer(track, tx.getFrom(), newContractAddress, endowment);
    }

    protected void go() {
        if (!readyToExecute) {
            return;
        }

        try {
            if (vm == null) { // no vm involved
                track.commit();
            } else {
                // charge basic cost of the transaction
                program.spendGas(basicTxCost, "basic transaction cost");

                vm.play(program);

                // overwrites result
                result = program.getResult();
                gasLeft = tx.getGas().subtract(toBI(result.getGasUsed()));

                if (tx.isCreate() && !result.isRevert()) {
                    int returnDataGasValue = getLength(result.getReturnData())
                            * config.getFeeSchedule().getCREATE_DATA();

                    if (gasLeft.compareTo(BigInteger.valueOf(returnDataGasValue)) < 0) {
                        // Not enough gas to return contract code
                        if (!config.getConstants().createEmptyContractOnOOG()) {
                            program.setRuntimeFailure(
                                    ExceptionFactory.notEnoughSpendingGas("No gas to return just created contract",
                                            returnDataGasValue, program));
                        }
                        result.setReturnData(EMPTY_BYTE_ARRAY);
                    } else if (getLength(result.getReturnData()) > config.getConstants().getMAX_CONTRACT_SZIE()) {
                        // Contract size too large
                        program.setRuntimeFailure(ExceptionFactory
                                .notEnoughSpendingGas("Contract size too large: " + getLength(result.getReturnData()),
                                        returnDataGasValue, program));
                        result.setReturnData(EMPTY_BYTE_ARRAY);
                    } else {
                        // Contract successfully created
                        gasLeft = gasLeft.subtract(BigInteger.valueOf(returnDataGasValue));
                        track.saveCode(HashUtil.calcNewAddress(tx.getFrom(), tx.getNonce()), result.getReturnData());
                    }
                }

                if (result.getException() != null || result.isRevert()) {
                    result.getDeleteAccounts().clear();
                    result.getLogs().clear();
                    result.resetFutureRefund();
                    rollback();

                    if (result.getException() != null) {
                        rollback();
                        logger.warn("Exception occurred", result.getException()); // defined exception
                    } else {
                        logger.warn("REVERT opcode executed");
                    }
                } else {
                    track.commit();
                }
            }
        } catch (Exception e) {
            rollback();
            logger.error("Unexpected exception", e); // unexpected, careful check required
        }
    }

    /**
     * Finalize all the changes to repository and builds a summary.
     *
     * @return a transaction summary, or NULL if the transaction fails the at
     *         {@link #init()}.
     */
    public TransactionSummary finish() {
        if (!readyToExecute) {
            return null;
        }

        // accumulate refunds for suicides
        result.addFutureRefund(result.getDeleteAccounts().size() * config.getFeeSchedule().getSUICIDE_REFUND());
        long gasRefund = Math.min(result.getFutureRefund(), getGasUsed() / 2);
        gasLeft = gasLeft.add(BigInteger.valueOf(gasRefund));

        // commit deleted accounts
        for (ByteArrayWrapper address : result.getDeleteAccounts()) {
            repo.delete(address.getData());
        }

        // refund
        BigInteger totalRefund = gasLeft.multiply(tx.getGasPrice());
        repo.addBalance(tx.getFrom(), gasLeft.multiply(tx.getGasPrice()));
        logger.info("Pay total refund to sender: amount = {}", totalRefund);

        return new TransactionSummary(tx, tx.getValue(), tx.getGas(), tx.getGasPrice(),
                BigInteger.valueOf(getGasUsed()),
                result.getException() != null,
                result.getReturnData(),
                result.getInternalTransactions(),
                new ArrayList<>(result.getDeleteAccounts()),
                result.getLogs());
    }

    private void rollback() {
        track.rollback();

        gasLeft = BigInteger.ZERO;
    }

    private long getGasUsed() {
        return tx.getGas().subtract(gasLeft).longValue();
    }
}
