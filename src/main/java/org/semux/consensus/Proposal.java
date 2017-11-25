/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.ArrayList;
import java.util.List;

import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Transaction;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class Proposal {

    private Proof proof;
    private BlockHeader blockHeader;
    private List<Transaction> transactions;

    private byte[] encoded;
    private Signature signature;

    public Proposal(Proof proof, BlockHeader blockHeader, List<Transaction> transactions) {
        // NOTE: the view of proposed block is always zero before being added to
        // blockchain, so do not check version match here.
        if (proof.getHeight() != blockHeader.getNumber()) {
            throw new RuntimeException("Proof-of-unlock and proposed block does not match");
        }

        this.proof = proof;
        this.blockHeader = blockHeader;
        this.transactions = transactions;

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(proof.toBytes());
        enc.writeBytes(blockHeader.toBytes());
        enc.writeInt(transactions.size());
        for (Transaction tx : transactions) {
            enc.writeBytes(tx.toBytes());
        }
        this.encoded = enc.toBytes();
    }

    public Proposal(byte[] encoded, byte[] signature) {
        SimpleDecoder dec = new SimpleDecoder(encoded);
        this.proof = Proof.fromBytes(dec.readBytes());
        this.blockHeader = BlockHeader.fromBytes(dec.readBytes());
        this.transactions = new ArrayList<>();
        int n = dec.readInt();
        for (int i = 0; i < n; i++) {
            transactions.add(Transaction.fromBytes(dec.readBytes()));
        }

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
     * the transactions inside the block. Use
     * {@link Block#validateHeader(BlockHeader, BlockHeader)} and
     * {@link Block#validateTransactions(BlockHeader, List)} for that purpose.
     * </p>
     * 
     * @return true if success, otherwise false
     */
    public boolean validate() {
        return getHeight() > 0//
                && getView() >= 0 //
                && proof != null //
                && encoded != null//
                && signature != null && EdDSA.verify(encoded, signature) //
                && blockHeader != null //
                && transactions != null;
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

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public List<Transaction> getTransactions() {
        return transactions;
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

    public static Proposal fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] encoded = dec.readBytes();
        byte[] signature = dec.readBytes();

        return new Proposal(encoded, signature);
    }

    @Override
    public String toString() {
        return "Proposal [height=" + getHeight() + ", view = " + getView() + ", # proof votes = "
                + proof.getVotes().size() + ", # txs = " + transactions.size() + "]";
    }
}
