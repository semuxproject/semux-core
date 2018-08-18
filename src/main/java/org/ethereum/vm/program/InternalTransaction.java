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
package org.ethereum.vm.program;

import java.math.BigInteger;

import org.ethereum.vm.OpCode;
import org.ethereum.vm.client.Transaction;

/**
 * Represents an internal transaction.
 */
public class InternalTransaction implements Transaction {

    private boolean rejected = false;

    private Transaction parentTx;
    private int depth;
    private int index;
    private OpCode type;

    private byte[] from;
    private byte[] to;
    private long nonce;
    private BigInteger value;
    private byte[] data;
    private BigInteger gastLimit;
    private BigInteger gasPrice;

    public InternalTransaction(Transaction parentTx, int depth, int index, OpCode type,
            byte[] from, byte[] to, long nonce, BigInteger value, byte[] data,
            BigInteger gas, BigInteger gasPrice) {
        this.parentTx = parentTx;
        this.depth = depth;
        this.index = index;
        this.type = type;

        this.from = from;
        this.to = to;
        this.nonce = nonce;
        this.value = value;
        this.data = data;
        this.gastLimit = gas;
        this.gasPrice = gasPrice;
    }

    public void reject() {
        this.rejected = true;
    }

    public boolean isRejected() {
        return rejected;
    }

    public Transaction getParentTransaction() {
        return parentTx;
    }

    public int getDepth() {
        return depth;
    }

    public int getIndex() {
        return index;
    }

    public OpCode getType() {
        return type;
    }

    @Override
    public boolean isCreate() {
        return type == OpCode.CREATE;
    }

    @Override
    public byte[] getFrom() {
        return from;
    }

    @Override
    public byte[] getTo() {
        return to;
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public BigInteger getValue() {
        return value;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public BigInteger getGas() {
        return gastLimit;
    }

    @Override
    public BigInteger getGasPrice() {
        return gasPrice;
    }

    @Override
    public String toString() {
        return "InternalTransaction [" +
                ", depth=" + getDepth() +
                ", index=" + getIndex() +
                ", type=" + getType() +
                "]";
    }

}
