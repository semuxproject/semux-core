package org.semux.core.state;

import org.semux.core.Amount;
import org.semux.util.SimpleDecoder;

public class AccountDecoderV2 implements AccountDecoder {

    @Override
    public AccountV2 decode(byte[] address, byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        Amount available = dec.readAmount();
        Amount locked = dec.readAmount();
        long nonce = dec.readLong();
        byte[] Abyte = dec.readBytes();

        return new AccountV2(Abyte, available, locked, nonce);
    }
}
