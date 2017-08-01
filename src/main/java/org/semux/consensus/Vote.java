/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class Vote {
    public static final byte VALUE_APPROVE = 0;
    public static final byte VALUE_REJECT = 1;

    private VoteType type;
    private byte value;

    private long height;
    private int view;
    private byte[] blockHash;

    private byte[] encoded;
    private Signature signature;

    public Vote(VoteType type, byte value, byte[] blockHash, long height, int view) {
        this.type = type;
        this.value = value;
        this.height = height;
        this.view = view;
        this.blockHash = blockHash;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeByte(type.toByte());
        enc.writeByte(value);
        enc.writeLong(height);
        enc.writeInt(view);
        enc.writeBytes(blockHash);
        this.encoded = enc.toBytes();
    }

    public Vote(byte[] encoded, byte[] signature) {
        this.encoded = encoded;

        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.type = VoteType.of(dec.readByte());
        this.value = dec.readByte();
        this.height = dec.readLong();
        this.view = dec.readInt();
        this.blockHash = dec.readBytes();

        this.signature = Signature.fromBytes(signature);
    }

    public static Vote newApprove(VoteType type, long height, int view, byte[] blockHash) {
        return new Vote(type, VALUE_APPROVE, blockHash, height, view);
    }

    public static Vote newReject(VoteType type, long height, int view) {
        return new Vote(type, VALUE_REJECT, Hash.EMPTY_H256, height, view);
    }

    /**
     * Sign this vote.
     * 
     * @param key
     * @return
     */
    public Vote sign(EdDSA key) {
        this.signature = key.sign(encoded);
        return this;
    }

    /**
     * validate the vote format and signature.
     * 
     * @return
     */
    public boolean validate() {
        return type != null //
                && (value == VALUE_APPROVE || value == VALUE_REJECT) //
                && height > 0 //
                && view >= 0 //
                && blockHash != null && blockHash.length == 32 //
                && encoded != null //
                && signature != null && EdDSA.verify(encoded, signature);
    }

    public VoteType getType() {
        return type;
    }

    public byte getValue() {
        return value;
    }

    public long getHeight() {
        return height;
    }

    public int getView() {
        return view;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public byte[] getEncoded() {
        return encoded;
    }

    public Signature getSignature() {
        return signature;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(encoded);
        enc.writeBytes(signature.toBytes());

        return enc.toBytes();
    }

    public static Vote fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] encoded = dec.readBytes();
        byte[] signature = dec.readBytes();

        return new Vote(encoded, signature);
    }

    @Override
    public String toString() {
        return "Vote [" + type + ", " + (value == 0 ? "approve" : "reject") + ", height=" + height + ", view=" + view
                + "]";
    }
}
