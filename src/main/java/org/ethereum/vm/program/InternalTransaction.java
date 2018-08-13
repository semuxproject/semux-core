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

import org.ethereum.vm.OpCode;
import org.ethereum.vm.client.Transaction;
import org.ethereum.vm.util.HexUtil;

/**
 * Represents an internal transaction.
 */
public class InternalTransaction implements Transaction {

    private boolean rejected = false;

    private byte[] parentHash;
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

    public InternalTransaction(byte[] parentHash, int depth, int index, OpCode type,
            byte[] from, byte[] to, long nonce, BigInteger value, byte[] data,
            BigInteger gas, BigInteger gasPrice) {
        this.parentHash = parentHash;
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

    public byte[] getParentHash() {
        return parentHash;
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
    public byte[] getHash() {
        // TODO: implement the hash of internal transaction
        return new byte[0];
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
                "  parentHash=" + HexUtil.toHexString(getParentHash()) +
                ", depth=" + getDepth() +
                ", index=" + getIndex() +
                ", type=" + getType() +
                "]";
    }

}
