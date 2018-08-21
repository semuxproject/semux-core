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

public class BlockHeaderMessage extends Message {

    private final BlockHeader header;

    public BlockHeaderMessage(BlockHeader header) {
        super(MessageCode.BLOCK_HEADER, null);

        this.header = header;

        this.body = header.toBytes();
    }

    public BlockHeaderMessage(byte[] body) {
        super(MessageCode.BLOCK_HEADER, null);

        this.header = BlockHeader.fromBytes(body);

        this.body = body;
    }

    public BlockHeader getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "BlockHeaderMessage [header=" + header + "]";
    }
}
