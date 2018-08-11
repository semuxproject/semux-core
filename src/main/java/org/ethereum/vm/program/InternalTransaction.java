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
import org.ethereum.vm.client.Transaction;
import org.ethereum.vm.util.VMUtils;

/**
 * Represents an internal transaction.
 */
public class InternalTransaction implements Transaction {

    private byte[] parentHash;
    private int deep;
    private int index;
    private String note;

    private boolean rejected = false;

    public InternalTransaction(byte[] parentHash, int deep, int index, byte[] nonce, DataWord gasPrice,
            DataWord gasLimit, byte[] sendAddress, byte[] receiveAddress, byte[] value, byte[] data, String note) {
        this.parentHash = parentHash;
        this.deep = deep;
        this.index = index;
        this.note = note;
    }

    public void reject() {
        this.rejected = true;
    }

    public byte[] getParentHash() {
        return parentHash;
    }

    public int getDeep() {
        return deep;
    }

    public int getIndex() {
        return index;
    }

    public String getNote() {
        return note;
    }

    public boolean isRejected() {
        return rejected;
    }

    @Override
    public String toString() {
        return "TransactionData [" +
                "  parentHash=" + VMUtils.toHexString(getParentHash()) +
                ", depth=" + getDeep() +
                ", index=" + getIndex() +
                ", note=" + getNote() +
                "]";
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
    public byte[] nonce() {
        return new byte[0];
    }

    @Override
    public byte[] getValue() {
        return new byte[0];
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public byte[] getGasLimit() {
        return new byte[0];
    }

    @Override
    public byte[] getGasPrice() {
        return new byte[0];
    }
}
