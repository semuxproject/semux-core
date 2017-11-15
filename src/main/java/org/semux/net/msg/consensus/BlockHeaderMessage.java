/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.core.BlockHeader;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class BlockHeaderMessage extends Message {

    private BlockHeader header;

    public BlockHeaderMessage(BlockHeader header) {
        super(MessageCode.GET_BLOCK_HEADER, null);

        this.header = header;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(header == null ? Bytes.EMPY_BYTES : header.toBytes());
        this.encoded = enc.toBytes();
    }

    public BlockHeaderMessage(byte[] encoded) {
        super(MessageCode.GET_BLOCK, null);

        this.encoded = encoded;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        byte[] bytes = dec.readBytes();
        this.header = (bytes.length == 0) ? null : BlockHeader.fromBytes(bytes);
    }

    public BlockHeader getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "BlockHeaderMessage [header=" + header + "]";
    }
}
