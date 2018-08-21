/*
 * Copyright (c) [2018] [ The Semux Developers ]
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

import org.ethereum.vm.DataWord;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.program.invoke.ProgramInvoke;

public class Storage implements Repository {

    private final Repository repository;

    public Storage(ProgramInvoke programInvoke) {
        this.repository = programInvoke.getRepository();
    }

    @Override
    public void createAccount(byte[] address) {
        repository.createAccount(address);
    }

    @Override
    public boolean exists(byte[] addr) {
        return repository.exists(addr);
    }

    @Override
    public void delete(byte[] addr) {
        repository.delete(addr);
    }

    @Override
    public long increaseNonce(byte[] addr) {
        return repository.increaseNonce(addr);
    }

    @Override
    public long setNonce(byte[] addr, long nonce) {
        return repository.setNonce(addr, nonce);
    }

    @Override
    public long getNonce(byte[] addr) {
        return repository.getNonce(addr);
    }

    @Override
    public void saveCode(byte[] addr, byte[] code) {
        repository.saveCode(addr, code);
    }

    @Override
    public byte[] getCode(byte[] addr) {
        return repository.getCode(addr);
    }

    @Override
    public void putStorageRow(byte[] addr, DataWord key, DataWord value) {
        repository.putStorageRow(addr, key, value);
    }

    @Override
    public DataWord getStorageRow(byte[] addr, DataWord key) {
        return repository.getStorageRow(addr, key);
    }

    @Override
    public BigInteger getBalance(byte[] addr) {
        return repository.getBalance(addr);
    }

    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {
        return repository.addBalance(addr, value);
    }

    @Override
    public Repository startTracking() {
        return repository.startTracking();
    }

    @Override
    public void commit() {
        repository.commit();
    }

    @Override
    public void rollback() {
        repository.rollback();
    }

}
