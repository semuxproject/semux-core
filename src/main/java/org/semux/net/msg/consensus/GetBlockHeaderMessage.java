/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class GetBlockHeaderMessage extends Message {
    private long number;

    public GetBlockHeaderMessage(long number) {
        super(MessageCode.GET_BLOCK_HEADER, BlockMessage.class);

        this.number = number;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(number);
        this.encoded = enc.toBytes();
    }

    public GetBlockHeaderMessage(byte[] encoded) {
        super(MessageCode.GET_BLOCK_HEADER, BlockMessage.class);

        this.encoded = encoded;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.number = dec.readLong();
    }

    public long getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "GetBlockHeaderMessage [number=" + number + "]";
    }
}
