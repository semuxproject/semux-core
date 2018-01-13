/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.util.Bytes;

public class NewHeightMessage extends Message {

    private long height;

    public NewHeightMessage(long height) {
        super(MessageCode.BFT_NEW_HEIGHT, null);
        this.height = height;

        this.encoded = Bytes.of(height);
    }

    public NewHeightMessage(byte[] encoded) {
        super(MessageCode.BFT_NEW_HEIGHT, null);
        this.encoded = encoded;

        this.height = Bytes.toLong(encoded);
    }

    public long getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "BFTNewHeightMessage [height=" + height + "]";
    }
}
