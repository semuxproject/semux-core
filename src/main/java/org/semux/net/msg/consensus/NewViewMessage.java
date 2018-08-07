/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.consensus.Proof;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;

public class NewViewMessage extends Message {

    private final Proof proof;

    public NewViewMessage(Proof proof) {
        super(MessageCode.BFT_NEW_VIEW, null);

        this.proof = proof;

        // FIXME: consider wrapping by simple codec
        this.body = proof.toBytes();
    }

    public NewViewMessage(byte[] body) {
        super(MessageCode.BFT_NEW_VIEW, null);

        this.proof = Proof.fromBytes(body);

        this.body = body;
    }

    public Proof getProof() {
        return proof;
    }

    public long getHeight() {
        return proof.getHeight();
    }

    public int getView() {
        return proof.getView();
    }

    @Override
    public String toString() {
        return "BFTNewViewMessage [proof=" + proof + "]";
    }
}