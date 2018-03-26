/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.semux.core.BlockHeaderData;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.semux.util.exception.UnreachableException;

/**
 * This class represents a Validator Activated Soft Fork inspired by Miner
 * Activated Soft Fork (MASF) in Bitcoin. See:
 * https://github.com/bitcoin/bips/blob/master/bip-0034.mediawiki
 */
public final class ValidatorActivatedFork implements Comparable<ValidatorActivatedFork> {

    /**
     * This soft fork introduces an uniformly-distributed hash function for choosing
     * primary validator. See: https://github.com/semuxproject/semux/issues/620
     */
    public static final ValidatorActivatedFork UNIFORM_DISTRIBUTION = new ValidatorActivatedFork((short) 1,
            "UNIFORM_DISTRIBUTION", 1500, 2000, 1000000);

    /**
     * An unique number of this fork.
     */
    public final short number;

    /**
     * The name of this fork.
     */
    public final String name;

    /**
     * The number of blocks which are required to have a fork number that is greater
     * than or equal to the number of this fork.
     */
    public final long activationBlocks;

    /**
     * The number of blocks to lookup for fork signal in block header.
     */
    public final long activationBlocksLookup;

    /**
     * The last block at which this block can be activated.
     */
    public final long activationDeadline;

    private ValidatorActivatedFork(short number, String name, long activationBlocks, long activationBlocksLookup,
            long activationDeadline) {
        this.number = number;
        this.name = name;
        this.activationBlocks = activationBlocks;
        this.activationBlocksLookup = activationBlocksLookup;
        this.activationDeadline = activationDeadline;
    }

    public byte[] toBytes() {
        SimpleEncoder simpleEncoder = new SimpleEncoder();
        simpleEncoder.writeShort(number);
        return simpleEncoder.toBytes();
    }

    public static ValidatorActivatedFork fromBytes(byte[] bytes) {
        SimpleDecoder simpleDecoder = new SimpleDecoder(bytes);
        short forkNumber = simpleDecoder.readShort();
        if (forkNumber == 1) {
            return UNIFORM_DISTRIBUTION;
        }
        return null;
    }

    @Override
    public int compareTo(ValidatorActivatedFork o) {
        return Long.compare(number, o.number);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ValidatorActivatedFork && compareTo((ValidatorActivatedFork) o) == 0;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(73, 103).append(number).build();
    }

    public static class Activation {

        public final ValidatorActivatedFork fork;

        public final long activatedAt;

        public Activation(ValidatorActivatedFork fork, long activatedAt) {
            this.fork = fork;
            this.activatedAt = activatedAt;
        }

        public byte[] toBytes() {
            SimpleEncoder simpleEncoder = new SimpleEncoder();
            simpleEncoder.writeBytes(fork.toBytes());
            simpleEncoder.writeLong(activatedAt);
            return simpleEncoder.toBytes();
        }

        public static Activation fromBytes(byte[] bytes) {
            SimpleDecoder simpleDecoder = new SimpleDecoder(bytes);
            ValidatorActivatedFork fork = ValidatorActivatedFork.fromBytes(simpleDecoder.readBytes());
            long activatedAt = simpleDecoder.readLong();
            return new Activation(fork, activatedAt);
        }
    }
}
