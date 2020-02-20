/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

/**
 * This class represents a Validator Activated Soft Fork inspired by Miner
 * Activated Soft Fork (MASF) in BIP-34.
 */
public enum Fork implements Comparable<Fork> {

    /**
     * This soft fork introduces an uniformly-distributed hash function for choosing
     * primary validator.
     */
    UNIFORM_DISTRIBUTION((short) 1, 1500, 2000),

    /**
     * This soft fork introduces the virtual machine.
     */
    VIRTUAL_MACHINE((short) 2, 1500, 2000),

    /**
     * This soft fork introduces an upgrade to the voting pre-compiled contracts.
     */
    VOTING_PRECOMPILED_UPGRADE((short) 3, 1500, 2000);

    /**
     * An unique number of this fork.
     */
    private short id;

    /**
     * The number of blocks which are required to activate this fork.
     */
    private int blocksRequired;

    /**
     * The number of blocks to check fork signal in block header.
     */
    private int blocksToCheck;

    Fork(short id, int blocksRequired, int blocksToCheck) {
        this.id = id;
        this.blocksRequired = blocksRequired;
        this.blocksToCheck = blocksToCheck;
    }

    public static Fork of(short id) {
        for (Fork f : Fork.values()) {
            if (f.id == id) {
                return f;
            }
        }
        return null;
    }

    public short id() {
        return id;
    }

    public int blocksRequired() {
        return blocksRequired;
    }

    public int blocksToCheck() {
        return blocksToCheck;
    }

    public static class Activation {

        public final Fork fork;

        public final long effectiveFrom;

        public Activation(Fork fork, long effectiveFrom) {
            this.fork = fork;
            this.effectiveFrom = effectiveFrom;
        }

        public byte[] toBytes() {
            SimpleEncoder encoder = new SimpleEncoder();
            encoder.writeShort(fork.id);
            encoder.writeLong(effectiveFrom);
            return encoder.toBytes();
        }

        public static Activation fromBytes(byte[] bytes) {
            SimpleDecoder decoder = new SimpleDecoder(bytes);
            Fork fork = Fork.of(decoder.readShort());
            long activatedAt = decoder.readLong();
            return new Activation(fork, activatedAt);
        }
    }
}
