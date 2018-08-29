package org.semux.vm.client;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.client.Repository;

import java.math.BigInteger;

public class SemuxRepository implements Repository {
    @Override
    public boolean isExist(byte[] address) {
        return false;
    }

    @Override
    public void createAccount(byte[] address) {

    }

    @Override
    public void delete(byte[] address) {

    }

    @Override
    public long increaseNonce(byte[] address) {
        return 0;
    }

    @Override
    public long setNonce(byte[] address, long nonce) {
        return 0;
    }

    @Override
    public long getNonce(byte[] address) {
        return 0;
    }

    @Override
    public void saveCode(byte[] address, byte[] code) {

    }

    @Override
    public byte[] getCode(byte[] address) {
        return new byte[0];
    }

    @Override
    public void putStorageRow(byte[] address, DataWord key, DataWord value) {

    }

    @Override
    public DataWord getStorageRow(byte[] address, DataWord key) {
        return null;
    }

    @Override
    public BigInteger getBalance(byte[] address) {
        return null;
    }

    @Override
    public BigInteger addBalance(byte[] address, BigInteger value) {
        return null;
    }

    @Override
    public Repository startTracking() {
        return null;
    }

    @Override
    public void commit() {

    }

    @Override
    public void rollback() {

    }
}
