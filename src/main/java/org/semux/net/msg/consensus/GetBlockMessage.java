/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.Bytes;

public class GetBlockMessage extends Message {
    private final long number;

    public GetBlockMessage(long number) {
        super(MessageCode.GET_BLOCK, BlockMessage.class);
        this.number = number;
        this.encoded = Bytes.of(number);
    }

    public GetBlockMessage(byte[] encoded) {
        super(MessageCode.GET_BLOCK, BlockMessage.class);
        this.encoded = encoded;
        this.number = Bytes.toLong(encoded);
    }

    public long getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "GetBlockMessage [number=" + number + "]";
    }
}
