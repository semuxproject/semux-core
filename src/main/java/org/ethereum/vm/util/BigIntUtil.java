/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
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
