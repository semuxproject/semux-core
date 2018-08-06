/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class GetBlockMessage extends Message {

    private final long number;

    public GetBlockMessage(long number) {
        super(MessageCode.GET_BLOCK, BlockMessage.class);
        this.number = number;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(number);
        this.encoded = enc.toBytes();
    }

    public GetBlockMessage(byte[] encoded) {
        super(MessageCode.GET_BLOCK, BlockMessage.class);

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.number = dec.readLong();

        this.encoded = encoded;
    }

    public long getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "GetBlockMessage [number=" + number + "]";
    }
}
