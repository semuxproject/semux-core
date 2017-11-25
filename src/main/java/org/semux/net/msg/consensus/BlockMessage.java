/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.core.Block;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class BlockMessage extends Message {

    private Block block;

    public BlockMessage(Block block) {
        super(MessageCode.BLOCK, null);

        this.block = block;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(block == null ? Bytes.EMPTY_BYTES : block.toBytesHeader());
        enc.writeBytes(block == null ? Bytes.EMPTY_BYTES : block.toBytesTransactions());
        enc.writeBytes(block == null ? Bytes.EMPTY_BYTES : block.toBytesResults());
        enc.writeBytes(block == null ? Bytes.EMPTY_BYTES : block.toBytesVotes());
        this.encoded = enc.toBytes();
    }

    public BlockMessage(byte[] encoded) {
        super(MessageCode.BLOCK, null);

        this.encoded = encoded;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        byte[] header = dec.readBytes();
        byte[] transactions = dec.readBytes();
        byte[] results = dec.readBytes();
        byte[] votes = dec.readBytes();

        if (header.length != 0) {
            this.block = Block.fromBytes(header, transactions, results, votes);
        }
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return "BlockMessage [block=" + block + "]";
    }
}
