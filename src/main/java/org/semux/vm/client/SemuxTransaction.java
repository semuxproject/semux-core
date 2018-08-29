package org.semux.vm.client;

import org.ethereum.vm.client.Transaction;

import java.math.BigInteger;

public class SemuxTransaction implements Transaction {

    org.semux.core.Transaction transaction;
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
        return null;//transaction.getValue().getNano();
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public BigInteger getGas() {
        return null;
    }

    @Override
    public BigInteger getGasPrice() {
        return null;
    }
}
