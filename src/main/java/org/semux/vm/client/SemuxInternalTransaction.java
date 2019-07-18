/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import java.util.Arrays;

import org.ethereum.vm.OpCode;
import org.ethereum.vm.program.InternalTransaction;
import org.semux.core.Amount;
import org.semux.crypto.Hex;

/**
 * Represents a Semux-flavored internal transaction.
 */
public class SemuxInternalTransaction {

    private boolean rejected;
    private int depth;
    private int index;
    private OpCode type;

    private byte[] from;
    private byte[] to;
    private long nonce;
    private Amount value;
    private byte[] data;
    private long gas;
    private Amount gasPrice;

    public SemuxInternalTransaction(InternalTransaction it) {
        this(it.isRejected(), it.getDepth(), it.getIndex(), it.getType(),
                it.getFrom(), it.getTo(), it.getNonce(),
                Conversion.weiToAmount(it.getValue()),
                it.getData(), it.getGas(),
                Conversion.weiToAmount(it.getGasPrice()));
    }

    public SemuxInternalTransaction(boolean rejected, int depth, int index, OpCode type, byte[] from, byte[] to,
            long nonce,
            Amount value, byte[] data, long gas, Amount gasPrice) {
        this.rejected = rejected;
        this.depth = depth;
        this.index = index;
        this.type = type;
        this.from = from;
        this.to = to;
        this.nonce = nonce;
        this.value = value;
        this.data = data;
        this.gas = gas;
        this.gasPrice = gasPrice;
    }

    public boolean isRejected() {
        return rejected;
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

    public byte[] getFrom() {
        return from;
    }

    public byte[] getTo() {
        return to;
    }

    public long getNonce() {
        return nonce;
    }

    public Amount getValue() {
        return value;
    }

    public byte[] getData() {
        return data;
    }

    public long getGas() {
        return gas;
    }

    public Amount getGasPrice() {
        return gasPrice;
    }

    @Override
    public String toString() {
        return "SemuxInternalTransaction{" +
                "rejected=" + rejected +
                ", depth=" + depth +
                ", index=" + index +
                ", type=" + type +
                ", from=" + Hex.encode(from) +
                ", to=" + Hex.encode(to) +
                ", nonce=" + nonce +
                ", value=" + value +
                ", data=" + Arrays.toString(data) +
                ", gas=" + gas +
                ", gasPrice=" + gasPrice +
                '}';
    }
}
