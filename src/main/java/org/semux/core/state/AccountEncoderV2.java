/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
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
