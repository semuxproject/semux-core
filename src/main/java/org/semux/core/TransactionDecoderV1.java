package org.semux.core;

import org.semux.Network;
import org.semux.util.SimpleDecoder;

public class TransactionDecoderV1 implements TransactionDecoder {

    @Override
    public Transaction decode(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] hash = dec.readBytes();
        byte[] encoded = dec.readBytes();
        byte[] signature = dec.readBytes();

        return new Transaction(hash, encoded, signature);
    }

    @Override
    public Transaction decodeUnsigned(byte[] bytes) {
        SimpleDecoder decoder = new SimpleDecoder(bytes);

        byte networkId = decoder.readByte();
        byte type = decoder.readByte();
        byte[] to = decoder.readBytes();
        Amount value = decoder.readAmount();
        Amount fee = decoder.readAmount();
        long nonce = decoder.readLong();
        long timestamp = decoder.readLong();
        byte[] data = decoder.readBytes();

        long gasPrice = 0;
        long gas = 0;

        TransactionType transactionType = TransactionType.of(type);
        if (TransactionType.CALL == transactionType || TransactionType.CREATE == transactionType) {
            gasPrice = decoder.readLong();
            gas = decoder.readLong();
        }

        return new Transaction(Network.of(networkId), transactionType, to, value, fee, nonce, timestamp, data,
                gasPrice, gas);
    }
}
