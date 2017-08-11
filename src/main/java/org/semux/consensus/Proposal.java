/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.semux.core.Block;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hex;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class Proposal {

    public static class Proof {
        public static final Proof NO_PROOF = new Proof(Collections.emptyList());

        private List<Vote> votes;

        public Proof(List<Vote> votes) {
            super();
            this.votes = votes;
        }

        public List<Vote> getVotes() {
            return votes;
        }

        @Override
        public String toString() {
            return votes.isEmpty() ? "Empty" : "Proof [# of votes=" + votes.size() + "]";
        }
    }

    private long height;
    private int view;
    private Block block;
    private Proof proof;

    private byte[] encoded;
    private Signature signature;

    private Boolean isBlockValid = null;

    public Proposal(long height, int view, Block block, Proof proof) {
        this.height = height;
        this.view = view;
        this.block = block;
        this.proof = proof;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(height);
        enc.writeInt(view);
        enc.writeBytes(block.toBytes());
        enc.writeInt(proof.getVotes().size());
        for (Vote v : proof.getVotes()) {
            enc.writeBytes(v.toBytes());
        }
        this.encoded = enc.toBytes();
    }

    public Proposal(byte[] encoded, byte[] signature) {
        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.height = dec.readLong();
        this.view = dec.readInt();
        this.block = Block.fromBytes(dec.readBytes());
        List<Vote> votes = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            votes.add(Vote.fromBytes(dec.readBytes()));
        }
        this.proof = new Proof(votes);

        this.encoded = encoded;
        this.signature = Signature.fromBytes(signature);
    }

    /**
     * Sign this proposal.
     * 
     * @param key
     * @return
     */
    public Proposal sign(EdDSA key) {
        this.signature = key.sign(encoded);
        return this;
    }

    /**
     * <p>
     * Validate proposal format and signature.
     * </p>
     *
     * <p>
     * NOTOE: this method will NOT validate the proposed block, nor the transactions
     * inside the block. Use {@link Block#validate()} for that purpose.
     * </p>
     * 
     * @return true if valid, otherwise false
     */
    public boolean validate() {
        return height > 0 //
                && view >= 0 //
                && proof != null //
                && encoded != null//
                && signature != null && EdDSA.verify(encoded, signature) //
                && block != null;
    }

    public long getHeight() {
        return height;
    }

    public int getView() {
        return view;
    }

    public Block getBlock() {
        return block;
    }

    public Proof getProof() {
        return proof;
    }

    public Signature getSignature() {
        return signature;
    }

    public Boolean isBlockValid() {
        return isBlockValid;
    }

    public void setBlockValid(Boolean isBlockValid) {
        this.isBlockValid = isBlockValid;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(encoded);
        enc.writeBytes(signature.toBytes());

        return enc.toBytes();
    }

    public static Proposal fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] encoded = dec.readBytes();
        byte[] signature = dec.readBytes();

        return new Proposal(encoded, signature);
    }

    @Override
    public String toString() {
        return "Proposal [height=" + height + ", view=" + view + ", block hash = "
                + Hex.encode(block.getHash()).substring(0, 16) + ", # txs = " + block.getTransactions().size()
                + ", proof=" + proof + "]";
    }
}
