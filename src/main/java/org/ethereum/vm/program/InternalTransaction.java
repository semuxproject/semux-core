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

import org.ethereum.vm.DataWord;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.client.Transaction;
import org.ethereum.vm.util.VMUtils;

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
    private DataWord value;
    private byte[] data;
    private DataWord gastLimit;
    private DataWord gasPrice;

    public InternalTransaction(byte[] parentHash, int depth, int index, OpCode type,
            byte[] sendAddress, byte[] receiveAddress, byte[] nonce, byte[] value, byte[] data,
            DataWord gasLimit, DataWord gasPrice) {
        this.parentHash = parentHash;
        this.depth = depth;
        this.index = index;
        this.type = type;
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

    // TODO: implement the following methods

    @Override
    public byte[] getHash() {
        return new byte[0];
    }

    @Override
    public byte[] getFrom() {
        return new byte[0];
    }

    @Override
    public byte[] getTo() {
        return new byte[0];
    }

    @Override
    public long nonce() {
        return 0;
    }

    @Override
    public DataWord getValue() {
        return null;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public DataWord getGasLimit() {
        return null;
    }

    @Override
    public DataWord getGasPrice() {
        return null;
    }

    @Override
    public String toString() {
        return "InternalTransaction [" +
                "  parentHash=" + VMUtils.toHexString(getParentHash()) +
                ", depth=" + getDepth() +
                ", index=" + getIndex() +
                ", type=" + getType() +
                "]";
    }

}
