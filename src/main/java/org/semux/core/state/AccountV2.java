/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;
import org.semux.crypto.Key;

public class AccountV2 extends AccountV1 {

    protected final byte[] Abyte;

    public AccountV2(byte[] Abyte, Amount available, Amount locked, long nonce) {
        super(Key.Address.fromAbyte(Abyte), available, locked, nonce);
        this.Abyte = Abyte;
    }

    public byte[] getAbyte() {
        return Abyte;
    }
}
