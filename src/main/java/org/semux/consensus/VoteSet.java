/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;

/**
 * <p>
 * A <code>VoteSet</code> contains all the votes for a specific height and view.
 * </p>
 * 
 * <p>
 * It's assumed that the votes have the same <code>blockHash</code>. In case
 * where the primary validator sends out two different block proposal, either or
 * neither should be approved by consensus.
 * </p>
 *
 */
public class VoteSet {

    private byte[] blockHash;

    private Map<String, Vote> approvals;
    private Map<String, Vote> rejections;
    private long height;
    private int view;

    private Set<String> validators;
    private int twoThirds;

    /**
     * Create a vote set.
     * 
     * @param height
     * @param view
     * @param validators
     */
    public VoteSet(long height, int view, List<String> validators) {
        this.approvals = new ConcurrentHashMap<>();
        this.rejections = new ConcurrentHashMap<>();
        this.height = height;
        this.view = view;

        this.validators = new HashSet<>(validators);
        this.twoThirds = (int) Math.ceil(validators.size() * 2.0 / 3.0);
    }

    /**
     * Add vote to this set if the height and view match.
     * 
     * NOTE: signature is not verified, use {@link Vote#validate()} instead.
     * 
     * @param vote
     * @return
     */
    public boolean addVote(Vote vote) {
        Signature sig = vote.getSignature();
        String peerId;

        if (vote.getHeight() == height //
                && vote.getView() == view //
                && vote.getBlockHash() != null //
                && vote.validate() //
                && (peerId = Hex.encode(Hash.h160(sig.getPublicKey()))) != null //
                && validators.contains(peerId)) {
            if (vote.getValue() == Vote.VALUE_APPROVE) {
                /*
                 * Only allow voting for one blockHash.
                 */
                if (blockHash == null || Arrays.equals(blockHash, vote.getBlockHash())) {
                    blockHash = vote.getBlockHash();
                    return approvals.put(peerId, vote) == null;
                } else {
                    return false;
                }
            } else {
                return rejections.put(peerId, vote) == null;
            }
        }

        return false;
    }

    /**
     * Add votes to this set, by iteratively calling {@link #addVote(Vote)}.
     * 
     * @param votes
     */
    public void addVotes(Collection<Vote> votes) {
        for (Vote v : votes) {
            addVote(v);
        }
    }

    /**
     * Whether a conclusion has been reached.
     * 
     * @return
     */
    public boolean isFinalized() {
        return isApproved() || isRejected();
    }

    /**
     * Whether the underlying block proposal is approved.
     * 
     * @return
     */
    public boolean isApproved() {
        return approvals.size() >= twoThirds;
    }

    /**
     * Whether the underlying block proposal is rejected.
     * 
     * @return
     */
    public boolean isRejected() {
        return rejections.size() >= twoThirds;
    }

    /**
     * Clear all the votes
     */
    public void clear() {
        approvals.clear();
        rejections.clear();
    }

    /**
     * Get all the approval votes.
     * 
     * @return
     */
    public List<Vote> getApprovals() {
        return new ArrayList<>(approvals.values());
    }

    /**
     * Get all the rejection votes.
     * 
     * @return
     */
    public List<Vote> getRejections() {
        return new ArrayList<>(rejections.values());
    }

    /**
     * Get the 2/3 number.
     * 
     * @return
     */
    public int getTwoThirds() {
        return twoThirds;
    }

    /**
     * Get the total number of votes.
     * 
     * @return
     */
    public int size() {
        return approvals.size() + rejections.size();
    }

    @Override
    public String toString() {
        return "[yes=" + approvals.size() + ", no=" + rejections.size() + "]";
    }
}
