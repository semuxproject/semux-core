/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;

public class VoteSet {

    // TODO group votes by blockHash?

    private Map<String, Vote> approvals;
    private Map<String, Vote> rejections;
    private long height;
    private int view;

    private Set<String> validators;
    private int twoThirds;

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
     * NOTE: signature is not verified.
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
                return approvals.put(peerId, vote) == null;
            } else {
                return rejections.put(peerId, vote) == null;
            }
        }

        return false;
    }

    /**
     * Add votes to this set, basic check will be enforced as {@code addVote(Vote)}.
     * 
     * @param votes
     */
    public void addVotes(Collection<Vote> votes) {
        for (Vote v : votes) {
            addVote(v);
        }
    }

    public boolean isFinalized() {
        return isApproved() || isRejected();
    }

    public boolean isApproved() {
        return approvals.size() >= twoThirds;
    }

    public boolean isRejected() {
        return rejections.size() >= twoThirds;
    }

    public void clear() {
        approvals.clear();
        rejections.clear();
    }

    public int size() {
        return approvals.size() + rejections.size();
    }

    public List<Vote> getApprovals() {
        return new ArrayList<>(approvals.values());
    }

    public List<Vote> getRejections() {
        return new ArrayList<>(rejections.values());
    }

    public int getTwoThirds() {
        return twoThirds;
    }

    @Override
    public String toString() {
        return "[yes=" + approvals.size() + ", no=" + rejections.size() + "]";
    }
}
