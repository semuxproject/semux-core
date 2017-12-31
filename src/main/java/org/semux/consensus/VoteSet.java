/**
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
import java.util.Optional;
import java.util.Set;

import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hex;
import org.semux.util.ByteArray;

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

        if (vote.getType() == type &&
                vote.getHeight() == height
                && vote.getView() == view
                && vote.getBlockHash() != null
                && vote.validate()
                && sig != null
                && validators.contains(Hex.encode(sig.getAddress()))) {
            String peerId = Hex.encode(sig.getAddress());

            if (vote.getValue() == Vote.VALUE_APPROVE) {
                ByteArray key = ByteArray.of(vote.getBlockHash());
                Map<String, Vote> map = approvals.computeIfAbsent(key, k -> new HashMap<>());
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
     * @return Number of votes added
     */
    public int addVotes(Collection<Vote> votes) {
        int n = 0;
        for (Vote v : votes) {
            n += addVote(v) ? 1 : 0;
        }
        return n;
    }

    /**
     * Returns whether the given block hash has been approved by +2/3 validators.
     * 
     * @param blockHash
     * @return
     */
    public boolean isApproved(byte[] blockHash) {
        Map<String, Vote> v = approvals.get(ByteArray.of(blockHash));
        return v != null && v.size() >= getTwoThirds();
    }

    /**
     * Returns whether this view is rejected.
     * 
     * @return
     */
    public boolean isRejected() {
        return rejections.size() >= getTwoThirds();
    }

    /**
     * Returns the blockHash which has been approved by +2/3 validators, if exists.
     * 
     * @return
     */
    public Optional<byte[]> anyApproved() {
        for (Map.Entry<ByteArray, Map<String, Vote>> e : approvals.entrySet()) {
            Map<String, Vote> v = e.getValue();
            if (v.size() >= getTwoThirds()) {
                return Optional.of(e.getKey().getData());
            }
        }

        return Optional.empty();
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
    public List<Vote> getApprovals(byte[] blockHash) {
        Map<String, Vote> map = approvals.get(ByteArray.of(blockHash));
        return map == null ? new ArrayList<>() : new ArrayList<>(map.values());
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
