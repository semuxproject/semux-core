/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.util.SimpleEncoder;

public class DelegateEncoderV1 implements DelegateEncoder {

    @Override
    public byte[] encode(Delegate delegate) {
        assert (delegate instanceof DelegateV1);
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(delegate.getName());
        enc.writeLong(delegate.getRegisteredAt());
        enc.writeAmount(delegate.getVotes());
        return enc.toBytes();
    }
}
