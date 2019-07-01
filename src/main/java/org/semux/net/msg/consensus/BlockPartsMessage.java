/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import java.util.ArrayList;
import java.util.List;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class BlockPartsMessage extends Message {

    private final long number;
    private final int parts;
    private final List<byte[]> data;

    public BlockPartsMessage(long number, int parts, List<byte[]> data) {
        super(MessageCode.BLOCK_PARTS, null);

        this.number = number;
        this.parts = parts;
        this.data = data;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(number);
        enc.writeInt(parts);
        enc.writeInt(data.size());
        for (byte[] b : data) {
            enc.writeBytes(b);
        }
        this.body = enc.toBytes();
    }

    public BlockPartsMessage(byte[] body) {
        super(MessageCode.BLOCK_PARTS, null);

        SimpleDecoder dec = new SimpleDecoder(body);
        this.number = dec.readLong();
        this.parts = dec.readInt();
        this.data = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            data.add(dec.readBytes());
        }

        this.body = body;
    }

    public long getNumber() {
        return number;
    }

    public int getParts() {
        return parts;
    }

    public List<byte[]> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "BlockPartsMessage [number=" + number + ", parts=" + parts + "]";
    }
}
