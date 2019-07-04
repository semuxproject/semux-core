/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import java.math.BigInteger;

import org.ethereum.vm.OpCode;
import org.ethereum.vm.client.Transaction;
import org.ethereum.vm.program.InternalTransaction;
import org.semux.core.Amount;

// TODO: introduce InternalTransaction interface

/**
 * Represents a Semux-flavored internal transaction.
 */
public class SemuxInternalTransaction extends InternalTransaction {

    public SemuxInternalTransaction(InternalTransaction it) {
        super(it.getParentTransaction(), it.getDepth(), it.getIndex(), it.getType(),
                it.getFrom(), it.getTo(), it.getNonce(), it.getValue(), it.getData(), it.getGas(), it.getGasPrice());
    }

    public SemuxInternalTransaction(Transaction parentTx, int depth, int index, OpCode type,
            byte[] from, byte[] to, long nonce, Amount value, byte[] data,
            long gas, Amount gasPrice) {
        super(parentTx, depth, index, type, from, to, nonce,
                Conversion.amountToWei(value),
                data, gas,
                Conversion.amountToWei(gasPrice));
    }

    /**
     * Returns the value in nanoSEM.
     *
     * @return
     */
    @Override
    public BigInteger getValue() {
        return Conversion.weiToAmount(super.getValue()).getBigInteger();
    }

    /**
     * Returns the gas price in nanoSEM.
     *
     * @return
     */
    @Override
    public BigInteger getGasPrice() {
        return Conversion.weiToAmount(super.getGasPrice()).getBigInteger();
    }
}
