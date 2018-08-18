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
package org.ethereum.vm.util;

import java.math.BigInteger;

import org.ethereum.vm.client.Repository;

public class BigIntUtil {
    public static BigInteger toBI(long data) {
        return BigInteger.valueOf(data);
    }

    public static boolean isPositive(BigInteger value) {
        return value.signum() > 0;
    }

    public static boolean isCovers(BigInteger covers, BigInteger value) {
        return !isNotCovers(covers, value);
    }

    public static boolean isNotCovers(BigInteger covers, BigInteger value) {
        return covers.compareTo(value) < 0;
    }

    public static void transfer(Repository repository, byte[] fromAddr, byte[] toAddr, BigInteger value) {
        repository.addBalance(fromAddr, value.negate());
        repository.addBalance(toAddr, value);
    }
}
