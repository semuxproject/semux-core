package org.semux.core;

public interface TransactionEncoder {

    byte[] encode(Transaction transaction);

    byte[] encodeUnsigned(Transaction transaction);

}
