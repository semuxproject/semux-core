package org.semux.vm.client;

import org.ethereum.vm.client.Transaction;

import java.math.BigInteger;

/**
 * Facade for Transaction -> Transaction
 */
public class SemuxTransaction implements Transaction {

    private final org.semux.core.Transaction transaction;
    private final BigInteger gas;
    private final BigInteger gasPrice;

    public SemuxTransaction(org.semux.core.Transaction transaction, BigInteger gas, BigInteger gasPrice) {
        this.transaction = transaction;
        this.gas = gas;
        this.gasPrice = gasPrice;
    }

    @Override
    public boolean isCreate() {
        return false;
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
        return gas;
    }

    @Override
    public BigInteger getGasPrice() {
        return gasPrice;
    }
}
