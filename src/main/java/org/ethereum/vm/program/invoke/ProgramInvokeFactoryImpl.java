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

import org.ethereum.vm.DataWord;
import org.ethereum.vm.client.Block;
import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;
import org.ethereum.vm.client.Transaction;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.util.VMUtil;

public class ProgramInvokeFactoryImpl implements ProgramInvokeFactory {

    @Override
    public ProgramInvoke createProgramInvoke(Transaction tx, Block block, Repository repository,
            BlockStore blockStore) {

        byte[] address = tx.isCreate() ? VMUtil.calcNewAddress(tx.getFrom(), tx.getNonce()) : tx.getTo();
        byte[] origin = tx.getFrom();
        byte[] caller = tx.getFrom();
        BigInteger gas = tx.getGas();
        BigInteger gasPrice = tx.getGasPrice();
        BigInteger callValue = tx.getValue();
        byte[] callData = tx.getData();

        byte[] prevHash = block.getParentHash();
        byte[] coinbase = block.getCoinbase();
        long timestamp = block.getTimestamp();
        long number = block.getNumber();
        BigInteger difficulty = block.getDifficulty();
        BigInteger gasLimit = block.getGasLimit();

        return new ProgramInvokeImpl(new DataWord(address), new DataWord(origin), new DataWord(caller),
                new DataWord(gas), new DataWord(gasPrice), new DataWord(callValue), callData,
                new DataWord(prevHash), new DataWord(coinbase), new DataWord(timestamp), new DataWord(number),
                new DataWord(difficulty), new DataWord(gasLimit),
                repository, blockStore, 0, false);
    }

    @Override
    public ProgramInvoke createProgramInvoke(Program program,
            DataWord callerAddress, DataWord toAddress,
            DataWord gas, DataWord value, byte[] data,
            Repository repository, BlockStore blockStore, boolean isStaticCall) {

        DataWord origin = program.getOriginAddress();
        DataWord gasPrice = program.getGasPrice();

        DataWord prevHash = program.getPrevHash();
        DataWord coinbase = program.getCoinbase();
        DataWord timestamp = program.getTimestamp();
        DataWord number = program.getNumber();
        DataWord difficulty = program.getDifficulty();
        DataWord gasLimit = program.getGasLimit();

        return new ProgramInvokeImpl(toAddress, origin, callerAddress, gas, gasPrice, value, data,
                prevHash, coinbase, timestamp, number, difficulty, gasLimit,
                repository, blockStore, program.getCallDepth() + 1, isStaticCall);
    }
}
