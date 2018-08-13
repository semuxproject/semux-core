/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/*
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

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.ethereum.vm.util.BigIntUtil.isCovers;
import static org.ethereum.vm.util.BigIntUtil.toBI;
import static org.ethereum.vm.util.BigIntUtil.transfer;
import static org.ethereum.vm.util.ByteArrayUtil.EMPTY_BYTE_ARRAY;
import static org.ethereum.vm.util.HexUtil.toHexString;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.VM;
import org.ethereum.vm.config.ByzantiumConfig;
import org.ethereum.vm.config.Config;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.exception.ExceptionFactory;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.util.ByteArrayWrapper;
import org.ethereum.vm.util.VMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionExecutor.class);

    Config config;

    private Transaction tx;
    private Repository track;
    private Repository cacheTrack;
    private BlockStore blockStore;
    private final long gasUsedInTheBlock;
    private boolean readyToExecute = false;
    private String execError;

    private ProgramInvokeFactory programInvokeFactory;
    private byte[] coinbase;

    private ProgramResult result = new ProgramResult();
    private Block currentBlock;

    private VM vm;
    private Program program;

    PrecompiledContracts.PrecompiledContract precompiledContract;

    BigInteger m_endGas = BigInteger.ZERO;
    long basicTxCost = 0;
    List<LogInfo> logs = null;

    private Set<ByteArrayWrapper> touchedAccounts = new HashSet<>();

    boolean localCall = false;

    public TransactionExecutor(Transaction tx, byte[] coinbase, Repository track, BlockStore blockStore,
            ProgramInvokeFactory programInvokeFactory, Block currentBlock) {

        this(tx, coinbase, track, blockStore, programInvokeFactory, currentBlock, 0, new ByzantiumConfig());
    }

    public TransactionExecutor(Transaction tx, byte[] coinbase, Repository track, BlockStore blockStore,
            ProgramInvokeFactory programInvokeFactory, Block currentBlock,
            long gasUsedInTheBlock,
            Config config) {

        this.tx = tx;
        this.coinbase = coinbase;
        this.track = track;
        this.cacheTrack = track.startTracking();
        this.blockStore = blockStore;
        this.programInvokeFactory = programInvokeFactory;
        this.currentBlock = currentBlock;
        this.gasUsedInTheBlock = gasUsedInTheBlock;
        this.m_endGas = tx.getGas();
        this.config = config;
    }

    private void execError(String err) {
        logger.warn(err);
        execError = err;
    }

    /**
     * Do all the basic validation, if the executor will be ready to run the
     * transaction at the end set readyToExecute = true
     */
    public void init() {
        basicTxCost = config.getTransactionCost(tx);

        if (localCall) {
            readyToExecute = true;
            return;
        }

        BigInteger txGas = tx.getGas();
        BigInteger curBlockGasLimit = currentBlock.getGasLimit();

        boolean cumulativeGasReached = txGas.add(BigInteger.valueOf(gasUsedInTheBlock))
                .compareTo(curBlockGasLimit) > 0;
        if (cumulativeGasReached) {
            execError("Too much gas used in this block: Require");
            return;
        }

        if (txGas.compareTo(BigInteger.valueOf(basicTxCost)) < 0) {
            execError(String.format("Not enough gas for transaction execution: Require: %s Got: %s", basicTxCost,
                    txGas));
            return;
        }

        long reqNonce = track.getNonce(tx.getFrom());
        long txNonce = tx.getNonce();
        if (reqNonce != txNonce) {
            execError(String.format("Invalid nonce: required: %s , tx.nonce: %s", reqNonce, txNonce));

            return;
        }

        BigInteger txGasCost = tx.getGasPrice().multiply(txGas);
        BigInteger totalCost = tx.getValue().add(txGasCost);
        BigInteger senderBalance = track.getBalance(tx.getFrom());

        if (!isCovers(senderBalance, totalCost)) {

            execError(String.format("Not enough cash: Require: %s, Sender cash: %s", totalCost, senderBalance));

            return;
        }

        readyToExecute = true;
    }

    public void execute() {

        if (!readyToExecute)
            return;

        if (!localCall) {
            track.increaseNonce(tx.getFrom());

            BigInteger txGasLimit = tx.getGas();
            BigInteger txGasCost = tx.getGasPrice().multiply(txGasLimit);
            track.addBalance(tx.getFrom(), txGasCost.negate());
        }

        if (tx.isCreate()) {
            create();
        } else {
            call();
        }
    }

    private void call() {
        if (!readyToExecute)
            return;

        byte[] targetAddress = tx.getTo();
        precompiledContract = PrecompiledContracts.getContractForAddress(new DataWord(targetAddress), config);

        if (precompiledContract != null) {
            long requiredGas = precompiledContract.getGasForData(tx.getData());

            BigInteger spendingGas = BigInteger.valueOf(requiredGas).add(BigInteger.valueOf(basicTxCost));

            if (!localCall && m_endGas.compareTo(spendingGas) < 0) {
                // no refund
                // no endowment
                execError("Out of Gas calling precompiled contract 0x" + toHexString(targetAddress) +
                        ", required: " + spendingGas + ", left: " + m_endGas);
                m_endGas = BigInteger.ZERO;
                return;
            } else {

                m_endGas = m_endGas.subtract(spendingGas);

                // FIXME: save return for vm trace
                Pair<Boolean, byte[]> out = precompiledContract.execute(tx.getData());

                if (!out.getLeft()) {
                    execError("Error executing precompiled contract 0x" + toHexString(targetAddress));
                    m_endGas = BigInteger.ZERO;
                    return;
                }
            }

        } else {

            byte[] code = track.getCode(targetAddress);
            if (isEmpty(code)) {
                m_endGas = m_endGas.subtract(BigInteger.valueOf(basicTxCost));
                result.spendGas(basicTxCost);
            } else {
                ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(tx, currentBlock, cacheTrack,
                        blockStore);

                this.vm = new VM(config);
                this.program = new Program(code, programInvoke, tx, config);
            }
        }

        BigInteger endowment = tx.getValue();
        transfer(cacheTrack, tx.getFrom(), targetAddress, endowment);

        touchedAccounts.add(new ByteArrayWrapper(targetAddress));
    }

    private void create() {
        byte[] newContractAddress = VMUtil.calcNewAddr(tx.getFrom(), tx.getNonce());

        if (cacheTrack.isExist(newContractAddress)) {
            execError(
                    "Trying to create a contract with existing contract address: 0x" + toHexString(newContractAddress));
            m_endGas = BigInteger.ZERO;
            return;
        }

        // In case of hashing collisions (for TCK tests only), check for any balance
        // before createAccount()
        BigInteger oldBalance = track.getBalance(newContractAddress);
        cacheTrack.addBalance(newContractAddress, oldBalance);
        cacheTrack.increaseNonce(newContractAddress);

        if (isEmpty(tx.getData())) {
            m_endGas = m_endGas.subtract(BigInteger.valueOf(basicTxCost));
            result.spendGas(basicTxCost);
        } else {
            ProgramInvoke programInvoke = programInvokeFactory
                    .createProgramInvoke(tx, currentBlock, cacheTrack, blockStore);

            this.vm = new VM(config);
            this.program = new Program(tx.getData(), programInvoke, tx, config);

            // reset storage if the contract with the same address already exists
            // TCK test case only - normally this is near-impossible situation in the real
            // network
            // TODO make via Trie.clear() without keyset
            // ContractDetails contractDetails =
            // program.getStorage().getContractDetails(newContractAddress);
            // for (DataWord key : contractDetails.getStorageKeys()) {
            // program.storageSave(key, DataWord.ZERO);
            // }
        }

        BigInteger endowment = tx.getValue();
        transfer(cacheTrack, tx.getFrom(), newContractAddress, endowment);

        touchedAccounts.add(new ByteArrayWrapper(newContractAddress));
    }

    public void go() {
        if (!readyToExecute)
            return;

        try {

            if (vm != null) {

                // Charge basic cost of the transaction
                program.spendGas(config.getTransactionCost(tx), "TRANSACTION COST");

                vm.play(program);

                result = program.getResult();
                m_endGas = tx.getGas().subtract(toBI(program.getResult().getGasUsed()));

                if (tx.isCreate() && !result.isRevert()) {
                    int returnDataGasValue = getLength(program.getResult().getHReturn())
                            * config.getFeeSchedule().getCREATE_DATA();
                    if (m_endGas.compareTo(BigInteger.valueOf(returnDataGasValue)) < 0) {
                        // Not enough gas to return contract code
                        if (!config.getConstants().createEmptyContractOnOOG()) {
                            program.setRuntimeFailure(
                                    ExceptionFactory.notEnoughSpendingGas("No gas to return just created contract",
                                            returnDataGasValue, program));
                            result = program.getResult();
                        }
                        result.setHReturn(EMPTY_BYTE_ARRAY);
                    } else if (getLength(result.getHReturn()) > config.getConstants().getMAX_CONTRACT_SZIE()) {
                        // Contract size too large
                        program.setRuntimeFailure(ExceptionFactory
                                .notEnoughSpendingGas("Contract size too large: " + getLength(result.getHReturn()),
                                        returnDataGasValue, program));
                        result = program.getResult();
                        result.setHReturn(EMPTY_BYTE_ARRAY);
                    } else {
                        // Contract successfully created
                        m_endGas = m_endGas.subtract(BigInteger.valueOf(returnDataGasValue));
                        cacheTrack.saveCode(VMUtil.calcNewAddr(tx.getFrom(), tx.getNonce()), result.getHReturn());
                    }
                }

                if (result.getException() != null || result.isRevert()) {
                    result.getDeleteAccounts().clear();
                    result.getLogInfoList().clear();
                    result.resetFutureRefund();
                    rollback();

                    if (result.getException() != null) {
                        throw result.getException();
                    } else {
                        execError("REVERT opcode executed");
                    }
                } else {
                    touchedAccounts.addAll(result.getTouchedAccounts());
                    cacheTrack.commit();
                }

            } else {
                cacheTrack.commit();
            }

        } catch (Throwable e) {

            // TODO: catch whatever they will throw on you !!!
            // https://github.com/ethereum/cpp-ethereum/blob/develop/libethereum/Executive.cpp#L241
            rollback();
            m_endGas = BigInteger.ZERO;
            execError(e.getMessage());
        }
    }

    private void rollback() {

        cacheTrack.rollback();

        // remove touched account
        touchedAccounts
                .remove(new ByteArrayWrapper(
                        tx.isCreate() ? VMUtil.calcNewAddr(tx.getFrom(), tx.getNonce()) : tx.getTo()));
    }

    public TransactionSummary finalization() {
        if (!readyToExecute)
            return null;

        TransactionSummary.Builder summaryBuilder = TransactionSummary.builderFor(tx)
                .gasLeftover(m_endGas)
                .logs(result.getLogInfoList())
                .result(result.getHReturn());

        if (result != null) {
            // Accumulate refunds for suicides
            result.addFutureRefund(result.getDeleteAccounts().size() * config.getFeeSchedule().getSUICIDE_REFUND());
            long gasRefund = Math.min(result.getFutureRefund(), getGasUsed() / 2);
            byte[] addr = tx.isCreate() ? VMUtil.calcNewAddr(tx.getFrom(), tx.getNonce()) : tx.getTo();
            m_endGas = m_endGas.add(BigInteger.valueOf(gasRefund));

            summaryBuilder
                    .gasUsed(toBI(result.getGasUsed()))
                    .gasRefund(toBI(gasRefund))
                    .deletedAccounts(result.getDeleteAccounts())
                    .internalTransactions(result.getInternalTransactions());

            if (result.getException() != null) {
                summaryBuilder.markAsFailed();
            }
        }

        TransactionSummary summary = summaryBuilder.build();

        // Refund for gas leftover
        track.addBalance(tx.getFrom(), summary.getLeftover().add(summary.getRefund()));
        logger.info("Pay total refund to sender: [{}], refund val: [{}]", toHexString(tx.getFrom()),
                summary.getRefund());

        if (result != null) {
            logs = result.getLogInfoList();
            // Traverse list of suicides
            for (ByteArrayWrapper address : result.getDeleteAccounts()) {
                track.delete(address.getData());
            }
        }

        return summary;
    }

    public ProgramResult getResult() {
        return result;
    }

    public long getGasUsed() {
        return tx.getGas().subtract(m_endGas).longValue();
    }
}
