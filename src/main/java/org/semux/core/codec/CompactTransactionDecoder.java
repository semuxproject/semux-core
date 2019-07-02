/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.codec;

import java.math.BigInteger;

import org.semux.core.Transaction;
import org.semux.core.state.AccountStateV2;
import org.semux.core.state.AccountV2;
import org.semux.crypto.Key;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;

public class CompactTransactionDecoder extends TransactionDecoderV1 {

    AccountStateV2 accountState;

    public CompactTransactionDecoder(AccountStateV2 accountState) {
        this.accountState = accountState;
    }

    @Override
    public Transaction decode(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] hash = dec.readBytes();
        byte[] encoded = dec.readBytes();
        byte[] accountIndexBytes = dec.readBytes();
        BigInteger accountIndex = Bytes.toBigInteger(accountIndexBytes);
        AccountV2 account = accountState.getAccountByIndex(accountIndex);
        byte[] s = dec.readBytes();
        Key.Signature signature = new Key.Signature(s, account.getAbyte());

        return new Transaction(hash, encoded, signature.toBytes());
    }
}
