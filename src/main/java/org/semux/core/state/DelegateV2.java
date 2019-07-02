/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;
import org.semux.crypto.Key;

public class DelegateV2 extends DelegateV1 {

    protected final byte[] Abyte;

    public DelegateV2(byte[] Abyte, byte[] name, long registeredAt, Amount votes) {
        super(Key.Address.fromAbyte(Abyte), name, registeredAt, votes);
        this.Abyte = Abyte;
    }

    public byte[] getAbyte() {
        return Abyte;
    }
}
