/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import java.math.BigInteger;

import org.semux.core.Amount;
import org.semux.core.Unit;

/**
 * Conversion between ETH and SEM. The idea is to make 1 SEM = 1 ETH from a
 * smart contract viewpoint.
 */
public class Conversion {

    private static final BigInteger TEN_POW_NINE = BigInteger.TEN.pow(9);

    public static Amount weiToAmount(BigInteger value) {
        BigInteger nanoSEM = value.divide(TEN_POW_NINE);
        return Unit.NANO_SEM.of(nanoSEM.longValue());
    }

    public static BigInteger amountToWei(Amount value) {
        return value.getBigInteger().multiply(TEN_POW_NINE);
    }

    public static BigInteger amountToWei(long nanoSEM) {
        return BigInteger.valueOf(nanoSEM).multiply(TEN_POW_NINE);
    }
}
