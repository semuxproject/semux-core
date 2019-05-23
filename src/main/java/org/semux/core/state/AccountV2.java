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
