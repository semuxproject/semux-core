/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semux.crypto.EdDSA.Signature;
import org.semux.util.ByteArray;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;

/**
 * <p>
 * A <code>VoteSet</code> contains all the votes for a specific height and view.
 * All <code>APPROVE</code> votes are grouped by block hash; <code>REJECT</code>
 * votes are not. This class is not thread-safe.
 * </p>
 *
 */
public class VoteSet {

    private Map<ByteArray, Map<String, Vote>> approvals;
    private Map<String, Vote> rejections;
    private VoteType type;
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
    public VoteSet(VoteType type, long height, int view, List<String> validators) {
        this.approvals = new HashMap<>();
        this.rejections = new HashMap<>();
        this.type = type;
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

        if (vote.getType() == type && //
                vote.getHeight() == height //
                && vote.getView() == view //
                && vote.getBlockHash() != null //
                && vote.validate() //
                && (peerId = Hex.encode(Hash.h160(sig.getPublicKey()))) != null //
                && validators.contains(peerId)) {
            if (vote.getValue() == Vote.VALUE_APPROVE) {
                ByteArray key = ByteArray.of(vote.getBlockHash());
                Map<String, Vote> map = approvals.get(key);
                if (map == null) {
                    map = new HashMap<>();
                    approvals.put(key, map);
                }
                return map.put(peerId, vote) == null;
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
        return isAnyApproved() != null || isRejected();
    }

    /**
     * Returns the blockHash which has been approved by +2/3 validators, if exists.
     * 
     * @return
     */
    public byte[] isAnyApproved() {
        for (ByteArray k : approvals.keySet()) {
            Map<String, Vote> v = approvals.get(k);
            if (v.size() >= twoThirds) {
                return k.getData();
            }
        }

        return null;
    }

    /**
     * Returns whether the given block hash has been approved by +2/3 validators.
     * 
     * @param blockHash
     * @return
     */
    public boolean isApproved(byte[] blockHash) {
        Map<String, Vote> v = approvals.get(ByteArray.of(blockHash));
        return v != null && v.size() >= twoThirds;
    }

    /**
     * Returns whether this view is rejected.
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
        for (Map<String, Vote> map : approvals.values()) {
            if (map.size() >= twoThirds) {
                return new ArrayList<>(map.values());
            }
        }

        throw new RuntimeException("Voteset is not approved!");
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
        int count = 0;
        for (Map<String, Vote> map : approvals.values()) {
            count = Math.max(count, map.size());
        }
        return "[" + count + ", " + rejections.size() + "]";
    }
}
