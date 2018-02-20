/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.BitSet;

import org.semux.consensus.ValidatorActivatedFork;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockHeaderData {

    private static Logger logger = LoggerFactory.getLogger(BlockHeaderData.class);

    public static final BlockHeaderData VERSION_0_HEADER_DATA = new BlockHeaderData();

    public static final Byte CURRENT_VERSION = 0x01;

    /**
     * Encoding version of this header data. (1 byte)
     */
    public final Byte version;

    /**
     * A set of fork signals. (4 bytes)
     */
    public final ForkSignal forkSignal;

    /**
     * Reserved data field. (27 bytes)
     */
    private byte[] reserved = {};

    /**
     * Version 1 of header data.
     *
     * @param version
     * @param forkSignal
     * @param reserved
     */
    public BlockHeaderData(Byte version, ForkSignal forkSignal, byte[] reserved) {
        this.version = version;
        this.forkSignal = forkSignal;
        this.reserved = reserved;
    }

    public BlockHeaderData(ForkSignal forkSignal) {
        this.version = CURRENT_VERSION;
        this.forkSignal = forkSignal;
    }

    /**
     * Version 0 of header data.
     */
    private BlockHeaderData() {
        this.version = 0x00;
        this.forkSignal = null;
    }

    /**
     * Unknown version of header data.
     *
     * @param version
     * @param reserved
     */
    private BlockHeaderData(Byte version, byte[] reserved) {
        this.version = version;
        this.forkSignal = null;
        this.reserved = reserved;
    }

    public boolean signalingFork(ValidatorActivatedFork fork) {
        return forkSignal != null && forkSignal.signalingFork(fork);
    }

    public byte[] toBytes() {
        if (version == 0x00) {
            return new SimpleEncoder().toBytes();
        } else if (version == 0x01) {
            SimpleEncoder simpleEncoder = new SimpleEncoder();
            simpleEncoder.writeByte(version);
            simpleEncoder.writeBytes(forkSignal.toBytes());
            simpleEncoder.writeBytes(reserved);
            return simpleEncoder.toBytes();
        } else {
            SimpleEncoder simpleEncoder = new SimpleEncoder();
            simpleEncoder.writeByte(version);
            simpleEncoder.writeBytes(reserved);
            return simpleEncoder.toBytes();
        }
    }

    public static BlockHeaderData fromBytes(byte[] bytes) {
        // consider 0-length header data as encoding version 0
        if (bytes == null || bytes.length == 0) {
            return VERSION_0_HEADER_DATA;
        }

        try {
            SimpleDecoder simpleDecoder = new SimpleDecoder(bytes);
            Byte versionDecoded = simpleDecoder.readByte();
            if (versionDecoded == 0x01) {
                ForkSignal forkSignalDecoded = new ForkSignal(simpleDecoder.readBytes());
                byte[] reservedDecoded = simpleDecoder.readBytes();
                return new BlockHeaderData(versionDecoded, forkSignalDecoded, reservedDecoded);
            } else {
                byte[] reservedDecoded = simpleDecoder.readBytes();
                return new BlockHeaderData(versionDecoded, reservedDecoded);
            }
        } catch (Exception ex) {
            logger.trace("Failed to decode BlockHeaderData, falling back to an empty header data.", ex);
            return VERSION_0_HEADER_DATA;
        }
    }

    public static class ForkSignal {

        static final int MAX_FORKS = 32;

        private BitSet bitSet;

        public ForkSignal(ValidatorActivatedFork... validatorActivatedForks) {
            bitSet = new BitSet(MAX_FORKS);
            for (ValidatorActivatedFork validatorActivatedFork : validatorActivatedForks) {
                bitSet.set(validatorActivatedFork.number);
            }
        }

        public ForkSignal(byte[] bytes) {
            bitSet = BitSet.valueOf(bytes);
        }

        boolean signalingFork(ValidatorActivatedFork fork) {
            return bitSet.get(fork.number);
        }

        byte[] toBytes() {
            return bitSet.toByteArray();
        }
    }
}
