/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.client.Transaction;
import org.semux.core.TransactionType;

import java.math.BigInteger;

/**
 * Facade for Transaction -> Transaction
 */
public class SemuxTransaction implements Transaction {

    private final org.semux.core.Transaction transaction;

    public SemuxTransaction(org.semux.core.Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public boolean isCreate() {
        return transaction.getType().equals(TransactionType.CREATE);
    }

    @Override
    public byte[] getFrom() {

        return transaction.getFrom();
    }

    @Override
    public byte[] getTo() {
        return transaction.getTo();
    }

    @Override
    public long getNonce() {
        return transaction.getNonce();
    }

    @Override
    public BigInteger getValue() {

        return transaction.getValue().getBigInteger();
    }

    @Override
    public byte[] getData() {
        return transaction.getData();
    }

    @Override
    public BigInteger getGas() {
        return transaction.getGas().getBigInteger();
    }

    @Override
    public BigInteger getGasPrice() {
        return transaction.getGasLimit().getBigInteger();
    }
}
