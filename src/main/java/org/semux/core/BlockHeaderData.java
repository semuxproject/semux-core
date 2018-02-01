/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.consensus.ValidatorActivatedFork;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class BlockHeaderData {

    public static final BlockHeaderData EMPTY_DATA = new BlockHeaderData();

    public final Short forkNumber;

    private byte[] reserved = {};

    public BlockHeaderData(short forkNumber, byte[] reserved) {
        this.forkNumber = forkNumber;
        this.reserved = reserved;
    }

    public BlockHeaderData(short forkNumber) {
        this.forkNumber = forkNumber;
    }

    private BlockHeaderData() {
        this.forkNumber = null;
    }

    public boolean forkActivated(ValidatorActivatedFork fork) {
        return this.forkNumber != null && this.forkNumber >= fork.number;
    }

    public byte[] toBytes() {
        if (this == EMPTY_DATA || forkNumber == null) {
            return new byte[] {};
        } else {
            SimpleEncoder simpleEncoder = new SimpleEncoder();
            simpleEncoder.writeShort(forkNumber);
            simpleEncoder.writeBytes(reserved);
            return simpleEncoder.toBytes();
        }
    }

    public static BlockHeaderData fromBytes(byte[] bytes) {
        try {
            SimpleDecoder simpleDecoder = new SimpleDecoder(bytes);
            short forkNumberDecoded = simpleDecoder.readShort();
            byte[] reservedDecoded = simpleDecoder.readBytes();
            return new BlockHeaderData(forkNumberDecoded, reservedDecoded);
        } catch (IndexOutOfBoundsException ex) {
            return EMPTY_DATA;
        }
    }
}
