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
package org.ethereum.vm.program.invoke;

import java.math.BigInteger;
import java.util.Objects;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.util.HexUtil;

public class ProgramInvokeImpl implements ProgramInvoke {

    /**
     * Transaction environment
     */
    private final DataWord address, origin, caller, gas, gasPrice, value;
    private final byte[] data;
    private final long gasLong;

    /**
     * Block environment
     */
    private final DataWord prevHash, coinbase, timestamp, number, difficulty, gaslimit;

    /**
     * Database environment
     */
    private final Repository repository;
    private final BlockStore blockStore;

    private int callDepth = 0;
    private boolean isStaticCall = false;

    public ProgramInvokeImpl(DataWord address, DataWord origin, DataWord caller,
            DataWord gas, DataWord gasPrice, DataWord value, byte[] data, DataWord prevHash,
            DataWord coinbase, DataWord timestamp, DataWord number, DataWord difficulty,
            DataWord gasLimit, Repository repository, BlockStore blockStore,
            int callDepth, boolean isStaticCall) {

        Objects.requireNonNull(address);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(caller);
        Objects.requireNonNull(gas);
        Objects.requireNonNull(gasPrice);
        Objects.requireNonNull(value);
        Objects.requireNonNull(data);

        Objects.requireNonNull(prevHash);
        Objects.requireNonNull(coinbase);
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(number);
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(gasLimit);

        Objects.requireNonNull(repository);
        Objects.requireNonNull(blockStore);

        this.address = address;
        this.origin = origin;
        this.caller = caller;
        this.gas = gas;
        this.gasLong = gas.longValueSafe();
        this.gasPrice = gasPrice;
        this.value = value;
        this.data = data;

        this.prevHash = prevHash;
        this.coinbase = coinbase;
        this.timestamp = timestamp;
        this.number = number;
        this.difficulty = difficulty;
        this.gaslimit = gasLimit;

        this.repository = repository;
        this.blockStore = blockStore;

        this.callDepth = callDepth;
        this.isStaticCall = isStaticCall;
    }

    @Override
    public DataWord getOwnerAddress() {
        return address;
    }

    @Override
    public DataWord getOriginAddress() {
        return origin;
    }

    @Override
    public DataWord getCallerAddress() {
        return caller;
    }

    @Override
    public DataWord getGas() {
        return gas;
    }

    @Override
    public long getGasLong() {
        return gasLong;
    }

    @Override
    public DataWord getGasPrice() {
        return gasPrice;
    }

    @Override
    public DataWord getValue() {
        return value;
    }

    // open for testing
    public byte[] getData() {
        return data;
    }

    @Override
    public DataWord getDataValue(DataWord indexData) {
        byte[] data = getData();

        BigInteger indexBI = indexData.value();
        if (indexBI.compareTo(BigInteger.valueOf(data.length)) >= 0) {
            return DataWord.ZERO;
        }

        int idx = indexBI.intValue();
        int size = Math.min(data.length - idx, DataWord.SIZE);

        byte[] buffer = new byte[DataWord.SIZE];
        System.arraycopy(data, idx, buffer, 0, size); // left-aligned

        return new DataWord(buffer);
    }

    @Override
    public DataWord getDataSize() {
        byte[] data = getData();

        return new DataWord(data.length);
    }

    @Override
    public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {
        byte[] data = getData();

        BigInteger offsetBI = offsetData.value();
        BigInteger lengthBI = lengthData.value();

        if (offsetBI.compareTo(BigInteger.valueOf(data.length)) >= 0) {
            return new byte[0];
        }

        int offset = offsetBI.intValue();
        int size = data.length - offset;
        if (lengthBI.compareTo(BigInteger.valueOf(size)) < 0) {
            size = lengthBI.intValue();
        }

        byte[] buffer = new byte[size];
        System.arraycopy(data, offset, buffer, 0, size);

        return buffer;
    }

    @Override
    public DataWord getPrevHash() {
        return prevHash;
    }

    @Override
    public DataWord getCoinbase() {
        return coinbase;
    }

    @Override
    public DataWord getTimestamp() {
        return timestamp;
    }

    @Override
    public DataWord getNumber() {
        return number;
    }

    @Override
    public DataWord getDifficulty() {
        return difficulty;
    }

    @Override
    public DataWord getGaslimit() {
        return gaslimit;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public BlockStore getBlockStore() {
        return blockStore;
    }

    @Override
    public int getCallDepth() {
        return this.callDepth;
    }

    @Override
    public boolean isStaticCall() {
        return isStaticCall;
    }

    @Override
    public String toString() {
        return "ProgramInvokeImpl{" +
                "address=" + address +
                ", origin=" + origin +
                ", caller=" + caller +
                ", gas=" + gas +
                ", gasPrice=" + gasPrice +
                ", value=" + value +
                ", data=" + HexUtil.toHexString(data) +
                ", gasLong=" + gasLong +
                ", prevHash=" + prevHash +
                ", coinbase=" + coinbase +
                ", timestamp=" + timestamp +
                ", number=" + number +
                ", difficulty=" + difficulty +
                ", gaslimit=" + gaslimit +
                ", repository=" + repository +
                ", blockStore=" + blockStore +
                ", callDepth=" + callDepth +
                ", isStaticCall=" + isStaticCall +
                '}';
    }
}
