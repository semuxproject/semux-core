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
package org.ethereum.vm.program;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.util.ByteArrayUtil;

/**
 * A data structure to hold the results of program.
 */
public class ProgramResult {

    private long gasUsed = 0;
    private byte[] hReturn = ByteArrayUtil.EMPTY_BYTE_ARRAY;
    private RuntimeException exception = null;
    private boolean revert = false;

    private Set<DataWord> deleteAccounts = new HashSet<>();
    private Set<DataWord> touchedAccounts = new HashSet<>();
    private List<InternalTransaction> internalTransactions = new ArrayList<>();
    private List<LogInfo> logInfoList = new ArrayList<>();
    private long futureRefund = 0;

    public long getGasUsed() {
        return gasUsed;
    }

    public void spendGas(long gas) {
        gasUsed += gas;
    }

    public void refundGas(long gas) {
        gasUsed -= gas;
    }

    public void setHReturn(byte[] hReturn) {
        this.hReturn = hReturn;

    }

    public byte[] getHReturn() {
        return hReturn;
    }

    public RuntimeException getException() {
        return exception;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    public void setRevert() {
        this.revert = true;
    }

    public boolean isRevert() {
        return revert;
    }

    public Set<DataWord> getDeleteAccounts() {
        return deleteAccounts;
    }

    public void addDeleteAccount(DataWord address) {
        deleteAccounts.add(address);
    }

    public void addDeleteAccounts(Set<DataWord> accounts) {
        deleteAccounts.addAll(accounts);
    }

    public void addTouchAccount(byte[] addr) {
        touchedAccounts.add(new DataWord(addr));
    }

    public Set<byte[]> getTouchedAccounts() {
        return touchedAccounts.stream().map(DataWord::getLast20Bytes).collect(Collectors.toSet());
    }

    public void addTouchAccounts(Set<byte[]> accounts) {
        for (byte[] a : accounts) {
            addTouchAccount(a);
        }
    }

    public List<LogInfo> getLogInfoList() {
        return logInfoList;
    }

    public void addLogInfo(LogInfo logInfo) {
        logInfoList.add(logInfo);
    }

    public void addLogInfos(List<LogInfo> logInfos) {
        for (LogInfo log : logInfos) {
            addLogInfo(log);
        }
    }

    public List<InternalTransaction> getInternalTransactions() {
        return internalTransactions;
    }

    public InternalTransaction addInternalTransaction(byte[] parentHash, int depth, OpCode type,
            byte[] senderAddress, byte[] receiveAddress, BigInteger nonce, byte[] value, byte[] data,
            DataWord gasLimit, DataWord gasPrice) {
        InternalTransaction tx = new InternalTransaction(parentHash, depth, internalTransactions.size(), type,
                senderAddress, receiveAddress, nonce, value, data, gasLimit, gasPrice);
        internalTransactions.add(tx);
        return tx;
    }

    public void addInternalTransactions(List<InternalTransaction> internalTransactions) {
        internalTransactions.addAll(internalTransactions);
    }

    public void rejectInternalTransactions() {
        for (InternalTransaction internalTx : internalTransactions) {
            internalTx.reject();
        }
    }

    public void addFutureRefund(long gasValue) {
        futureRefund += gasValue;
    }

    public long getFutureRefund() {
        return futureRefund;
    }

    public void resetFutureRefund() {
        futureRefund = 0;
    }

    public void merge(ProgramResult another) {
        addInternalTransactions(another.getInternalTransactions());
        if (another.getException() == null && !another.isRevert()) {
            addDeleteAccounts(another.getDeleteAccounts());
            addLogInfos(another.getLogInfoList());
            addFutureRefund(another.getFutureRefund());
            addTouchAccounts(another.getTouchedAccounts());
        }
    }

    public static ProgramResult createEmpty() {
        return new ProgramResult();
    }
}
