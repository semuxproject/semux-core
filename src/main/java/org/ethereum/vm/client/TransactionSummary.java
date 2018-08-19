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

import java.math.BigInteger;
import java.util.List;

import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.InternalTransaction;
import org.ethereum.vm.util.ByteArrayWrapper;

public class TransactionSummary {

    private Transaction tx;
    private BigInteger value;
    private BigInteger gas;
    private BigInteger gasPrice;
    private BigInteger gasUsed;

    private boolean failed;
    private byte[] returnData;
    private List<InternalTransaction> internalTransactions;
    private List<ByteArrayWrapper> deletedAccounts;
    private List<LogInfo> logs;

    public TransactionSummary(Transaction tx, BigInteger value, BigInteger gas, BigInteger gasPrice,
            BigInteger gasUsed, boolean failed, byte[] returnData,
            List<InternalTransaction> internalTransactions,
            List<ByteArrayWrapper> deletedAccounts,
            List<LogInfo> logs) {
        this.tx = tx;
        this.value = value;
        this.gas = gas;
        this.gasPrice = gasPrice;
        this.gasUsed = gasUsed;
        this.failed = failed;
        this.returnData = returnData;
        this.internalTransactions = internalTransactions;
        this.deletedAccounts = deletedAccounts;
        this.logs = logs;
    }

    public Transaction getTx() {
        return tx;
    }

    public BigInteger getValue() {
        return value;
    }

    public BigInteger getGas() {
        return gas;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public BigInteger getGasUsed() {
        return gasUsed;
    }

    public boolean isFailed() {
        return failed;
    }

    public byte[] getReturnData() {
        return returnData;
    }

    public List<InternalTransaction> getInternalTransactions() {
        return internalTransactions;
    }

    public List<ByteArrayWrapper> getDeletedAccounts() {
        return deletedAccounts;
    }

    public List<LogInfo> getLogs() {
        return logs;
    }
}
