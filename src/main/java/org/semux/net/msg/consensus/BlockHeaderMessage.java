/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.core.BlockHeader;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class BlockHeaderMessage extends Message {

    private final BlockHeader header;

    public BlockHeaderMessage(BlockHeader header) {
        super(MessageCode.BLOCK_HEADER, null);

        this.header = header;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(header.toBytes());
        this.encoded = enc.toBytes();
    }

    public BlockHeaderMessage(byte[] encoded) {
        super(MessageCode.BLOCK_HEADER, null);

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.header = BlockHeader.fromBytes(dec.readBytes());

        this.encoded = encoded;
    }

    public BlockHeader getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "BlockHeaderMessage [header=" + header + "]";
    }
}
