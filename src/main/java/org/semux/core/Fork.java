/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

/**
 * This class represents a Validator Activated Soft Fork inspired by Miner
 * Activated Soft Fork (MASF) in Bitcoin. See:
 * https://github.com/bitcoin/bips/blob/master/bip-0034.mediawiki
 */
public final class Fork implements Comparable<Fork> {

    /**
     * This soft fork introduces an uniformly-distributed hash function for choosing
     * primary validator.
     */
    public static final Fork UNIFORM_DISTRIBUTION = new Fork((short) 1,
            "UNIFORM_DISTRIBUTION", 1500, 2000, 1000000);

    public static final Fork VIRTUAL_MACHINE = new Fork((short) 2,
            "VIRTUAL_MACHINE", 1500, 2000, 1500000);
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

    private Fork(short number, String name, long activationBlocks, long activationBlocksLookup,
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

    public static Fork fromBytes(byte[] bytes) {
        SimpleDecoder simpleDecoder = new SimpleDecoder(bytes);
        short forkNumber = simpleDecoder.readShort();
        if (forkNumber == 1) {
            return UNIFORM_DISTRIBUTION;
        }
        if (forkNumber == 2) {
            return VIRTUAL_MACHINE;
        }
        return null;
    }

    @Override
    public int compareTo(Fork o) {
        return Long.compare(number, o.number);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fork && compareTo((Fork) o) == 0;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(73, 103).append(number).build();
    }

    @Override
    public String toString() {
        return name;
    }

    public static class Activation {

        public final Fork fork;

        public final long activatedAt;

        public Activation(Fork fork, long activatedAt) {
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
            Fork fork = Fork.fromBytes(simpleDecoder.readBytes());
            long activatedAt = simpleDecoder.readLong();
            return new Activation(fork, activatedAt);
        }
    }
}
