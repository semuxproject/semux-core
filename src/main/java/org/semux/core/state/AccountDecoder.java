package org.semux.core.state;

public interface AccountDecoder {

    Account decode(byte[] address, byte[] bytes);
}
