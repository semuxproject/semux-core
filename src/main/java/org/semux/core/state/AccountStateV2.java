/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import java.math.BigInteger;

public interface AccountStateV2 extends AccountState {

    BigInteger getAccountIndex(byte[] accountAddress);

    BigInteger getAccountCount();

    AccountV2 getAccountByIndex(BigInteger index);
}
