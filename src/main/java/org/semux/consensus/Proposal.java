/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import org.semux.core.Block;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class Proposal {

    private Proof proof;
    private Block block;

    private byte[] encoded;
    private Signature signature;

    // block validation result cache
    private Boolean isBlockValid = null;

    public Proposal(Proof proof, Block block) {
        // NOTE: the view of proposed block is always zero before being added to
        // blockchain, so do not check version match here.
        if (proof.getHeight() != block.getNumber()) {
            throw new RuntimeException("Proof-of-unlock and proposed block does not match");
        }

        this.proof = proof;
        this.block = block;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(proof.toBytes());
        enc.writeBytes(block.toBytesHeader());
        enc.writeBytes(block.toBytesTransactions());
        enc.writeBytes(block.toBytesResults());
        this.encoded = enc.toBytes();
    }

    public Proposal(byte[] encoded, byte[] signature) {
        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.proof = Proof.fromBytes(dec.readBytes());
        byte[] header = dec.readBytes();
        byte[] transactions = dec.readBytes();
        byte[] results = dec.readBytes();
        this.block = Block.fromBytes(header, transactions, results);

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
     * NOTOE: this method will NOT validate the proposed block, nor the proof, nor
     * the transactions inside the block. Use {@link Block#validate()} for that
     * purpose.
     * </p>
     * 
     * @return true if valid, otherwise false
     */
    public boolean validate() {
        return getHeight() > 0//
                && getView() >= 0 //
                && proof != null //
                && encoded != null//
                && signature != null && EdDSA.verify(encoded, signature) //
                && block != null;
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

    public Block getBlock() {
        return block;
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
        return "Proposal [height=" + getHeight() + ", view = " + getView() + ", # proof votes = "
                + proof.getVotes().size() + ", # txs = " + block.getTransactions().size() + "]";
    }
}
