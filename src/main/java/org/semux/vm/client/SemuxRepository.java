package org.semux.vm.client;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.client.Repository;
import org.semux.core.Amount;
import org.semux.core.state.AccountState;

import java.math.BigInteger;

/**
 * Facade class for AccountState -> Repository
 *
 * We will probably want to make AccountState just implement repository
 * but for ease of initial integration, use a facade to limit scope
 */
public class SemuxRepository implements Repository {
    AccountState accountState;

    public SemuxRepository(AccountState accountState) {
        this.accountState = accountState;
    }

    @Override
    public boolean isExist(byte[] address) {
        return accountState.exists(address);
    }

    @Override
    public void createAccount(byte[] address) {
        if(!isExist(address))
        {
            accountState.setCode(address, new byte[]{});
        }
    }

    @Override
    public void delete(byte[] address) {
        if(isExist(address)) {
            accountState.setCode(address, null);
        }
    }

    @Override
    public long increaseNonce(byte[] address) {
        return accountState.increaseNonce(address);
    }

    @Override
    public long setNonce(byte[] address, long nonce) {
        return accountState.setNonce(address, nonce);
    }

    @Override
    public long getNonce(byte[] address) {
        return accountState.getAccount(address).getNonce();
    }

    @Override
    public void saveCode(byte[] address, byte[] code) {
        accountState.setCode(address, code);
    }

    @Override
    public byte[] getCode(byte[] address) {
        return accountState.getCode(address);
    }

    @Override
    public void putStorageRow(byte[] address, DataWord key, DataWord value) {
        accountState.putStorage(address, key.getData(), value.getData());
    }

    @Override
    public DataWord getStorageRow(byte[] address, DataWord key) {
        byte[] data = accountState.getStorage(address, key.getData());
        if(data != null) {
            return new DataWord(data);
        }
        return null;
    }

    @Override
    public BigInteger getBalance(byte[] address) {
        return accountState.getAccount(address).getAvailable().getBigInteger();
    }

    @Override
    public BigInteger addBalance(byte[] address, BigInteger value) {
        //todo - figure out BigInt changes
        accountState.adjustAvailable(address, Amount.Unit.NANO_SEM.of(value.longValue()));
        return value;
    }

    @Override
    public Repository startTracking() {
        return new SemuxRepository(accountState.track());
    }

    @Override
    public void commit() {
        accountState.commit();
    }

    @Override
    public void rollback() {
        accountState.rollback();
    }
}
