/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;
import org.semux.util.SimpleDecoder;

public class AccountDecoderV1 implements AccountDecoder {

    @Override
    public AccountV1 decode(byte[] address, byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        Amount available = dec.readAmount();
        Amount locked = dec.readAmount();
        long nonce = dec.readLong();

        return new AccountV1(address, available, locked, nonce);
    }
}
