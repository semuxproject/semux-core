package org.semux.core;

public interface TransactionDecoder {

    Transaction decode(byte[] bytes);

    Transaction decodeUnsigned(byte[] bytes);
}
