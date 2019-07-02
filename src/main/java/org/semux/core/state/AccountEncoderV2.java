package org.semux.core.state;

import org.semux.util.SimpleEncoder;

public class AccountEncoderV2 implements AccountEncoder {

    @Override
    public byte[] encode(Account account) {
        assert (account instanceof AccountV2);

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeAmount(account.getAvailable());
        enc.writeAmount(account.getLocked());
        enc.writeLong(account.getNonce());
        enc.writeBytes(((AccountV2) account).getAbyte());

        return enc.toBytes();
    }
}
