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

import org.ethereum.vm.DataWord;
import org.ethereum.vm.client.BlockStore;
import org.ethereum.vm.client.Repository;

/**
 * Represents a program invoke.
 */
public interface ProgramInvoke {

    // ===========================
    // Transaction context
    // ===========================

    DataWord getOwnerAddress();

    DataWord getOriginAddress();

    DataWord getCallerAddress();

    DataWord getGas();

    /**
     * Returns the gas as a long integer.
     *
     * @return the gas value, or {@link Long#MAX_VALUE} in case of overflow
     */
    long getGasLong();

    DataWord getGasPrice();

    DataWord getValue();

    DataWord getDataSize();

    DataWord getDataValue(DataWord index);

    byte[] getDataCopy(DataWord offset, DataWord length);

    // ===========================
    // Block context
    // ===========================

    DataWord getPrevHash();

    DataWord getCoinbase();

    DataWord getTimestamp();

    DataWord getNumber();

    DataWord getDifficulty();

    DataWord getGaslimit();

    // ===========================
    // Database context
    // ===========================

    Repository getRepository();

    BlockStore getBlockStore();

    // ===========================
    // Miscellaneous
    // ===========================

    int getCallDepth();

    boolean isStaticCall();
}
