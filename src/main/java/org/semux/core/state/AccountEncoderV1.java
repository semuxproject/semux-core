package org.semux.core.state;

import org.semux.util.SimpleEncoder;

public class AccountEncoderV1 implements AccountEncoder {
    @Override
    public byte[] encode(Account account) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeAmount(account.getAvailable());
        enc.writeAmount(account.getLocked());
        enc.writeLong(account.getNonce());

        return enc.toBytes();
    }
}
