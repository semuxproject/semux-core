/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file LICENSE or
 * https://opensource.org/licenses/mit-license.php
 */
/*
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
package org.ethereum.vm;

import org.apache.commons.lang3.tuple.Pair;

public class PrecompiledContracts {

    private static final Identity identity = new Identity();

    private static final DataWord identityAddr = new DataWord(
            "0000000000000000000000000000000000000000000000000000000000000004");

    public static PrecompiledContract getContractForAddress(DataWord address) {
        if (address == null)
            return identity;
        if (address.equals(identityAddr))
            return identity;

        return null;
    }

    public static abstract class PrecompiledContract {
        public abstract long getGasForData(byte[] data);

        public abstract Pair<Boolean, byte[]> execute(byte[] data);
    }

    public static class Identity extends PrecompiledContract {

        public Identity() {
        }

        @Override
        public long getGasForData(byte[] data) {
            // gas charge for the execution:
            // minimum 1 and additional 1 for each 32 bytes word (round  up)
            if (data == null)
                return 15;
            return 15 + (data.length + 31) / 32 * 3;
        }

        @Override
        public Pair<Boolean, byte[]> execute(byte[] data) {
            return Pair.of(true, data);
        }
    }
}
