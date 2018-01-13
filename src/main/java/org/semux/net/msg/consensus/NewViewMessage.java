/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.msg.consensus;

import org.semux.consensus.Proof;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;

public class NewViewMessage extends Message {

    private Proof proof;

    public NewViewMessage(Proof proof) {
        super(MessageCode.BFT_NEW_VIEW, null);

        this.encoded = proof.toBytes();
    }

    public NewViewMessage(byte[] encoded) {
        super(MessageCode.BFT_NEW_VIEW, null);
        this.encoded = encoded;

        this.proof = Proof.fromBytes(encoded);
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