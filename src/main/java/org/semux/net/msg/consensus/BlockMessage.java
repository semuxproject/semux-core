/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.core.Block;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class BlockMessage extends Message {

    private final Block block;

    public BlockMessage(Block block) {
        super(MessageCode.BLOCK, null);

        this.block = block;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(block.toBytesHeader());
        enc.writeBytes(block.toBytesTransactions());
        enc.writeBytes(block.toBytesResults());
        enc.writeBytes(block.toBytesVotes());
        this.encoded = enc.toBytes();
    }

    public BlockMessage(byte[] encoded) {
        super(MessageCode.BLOCK, null);

        SimpleDecoder dec = new SimpleDecoder(encoded);
        byte[] header = dec.readBytes();
        byte[] transactions = dec.readBytes();
        byte[] results = dec.readBytes();
        byte[] votes = dec.readBytes();
        this.block = Block.fromBytes(header, transactions, results, votes);

        this.encoded = encoded;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return "BlockMessage [block=" + block + "]";
    }
}
