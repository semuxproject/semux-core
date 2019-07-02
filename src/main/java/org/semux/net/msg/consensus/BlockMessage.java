/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.core.Block;
import org.semux.core.codec.BlockDecoder;
import org.semux.core.codec.BlockDecoderV1;
import org.semux.core.codec.BlockEncoderV1;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;

public class BlockMessage extends Message {

    private final Block block;

    public BlockMessage(Block block) {
        super(MessageCode.BLOCK, null);

        this.block = block;

        this.body = new BlockEncoderV1().encode(block);
    }

    public BlockMessage(byte[] body) {
        super(MessageCode.BLOCK, null);

        BlockDecoder blockDecoder = new BlockDecoderV1();
        this.block = blockDecoder.decode(body);

        this.body = body;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return "BlockMessage [block=" + block + "]";
    }
}
