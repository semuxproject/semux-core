/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.codec;

import org.semux.core.Transaction;
import org.semux.core.TransactionType;
import org.semux.util.SimpleEncoder;

public class TransactionEncoderV1 implements TransactionEncoder {
    @Override
    public byte[] encode(Transaction transaction) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(transaction.getHash());
        enc.writeBytes(transaction.getEncoded());
        enc.writeBytes(transaction.getSignature().toBytes()); // TODO use account index
        return enc.toBytes();
    }

    @Override
    public byte[] encodeUnsigned(Transaction transaction) {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeByte(transaction.getNetworkId());
        enc.writeByte(transaction.getType().toByte());
        enc.writeBytes(transaction.getTo());
        enc.writeAmount(transaction.getValue());
        enc.writeAmount(transaction.getFee());
        enc.writeLong(transaction.getNonce());
        enc.writeLong(transaction.getTimestamp());
        enc.writeBytes(transaction.getData());

        if (TransactionType.CALL == transaction.getType() || TransactionType.CREATE == transaction.getType()) {
            enc.writeLong(transaction.getGas());
            enc.writeLong(transaction.getGasPrice());
        }

        return enc.toBytes();
    }
}
