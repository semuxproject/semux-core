/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;
import org.semux.util.SimpleDecoder;

public class DelegateDecoderV1 implements DelegateDecoder {

    @Override
    public DelegateV1 decode(byte[] address, byte[] bytes) {
        assert (address.length == 20);

        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] name = dec.readBytes();
        long registeredAt = dec.readLong();
        Amount votes = dec.readAmount();

        return new DelegateV1(address, name, registeredAt, votes);
    }
}
