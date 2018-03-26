/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semux.consensus.ValidatorActivatedFork;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoded {@link BlockHeader#getData()}.
 *
 * This class follows factory method pattern. Instances of this class should
 * only be initiated by one of the following methods:
 * <ul>
 * <li>{@link BlockHeaderData#v0()}</li>
 * <li>{@link BlockHeaderData#v1(ForkSignalSet)}</li>
 * <li>{@link BlockHeaderData#unrecognized(byte, byte[])}}</li>
 * </ul>
 */
public class BlockHeaderData {

    public static final int MAX_SIZE = 32;

    private static Logger logger = LoggerFactory.getLogger(BlockHeaderData.class);

    private static final BlockHeaderData VERSION_0_HEADER_DATA = BlockHeaderData.v0();

    /**
     * Encoding version of this header data.
     */
    public final Byte version;

    /**
     * A set of fork signals. 8 pending forks at maximum.
     */
    private final ForkSignalSet forkSignalSet;

    /**
     * Reserved data field for unrecognized versions of header data.
     */
    private final byte[] reserved;

    /**
     * Version 0 of header data, which is always a 0-length byte array.
     */
    public static BlockHeaderData v0() {
        return new BlockHeaderData((byte) 0x00, new byte[0]);
    }

    /**
     * Version 1 of header data is composed with a version byte + a short array with
     * a length from 0 to 8.
     * <ul>
     * <li>Minimum encoded length: writeByte(1) + writeSize(1) + (writeByte(1) +
     * writeShort(2) * 0) = 3 bytes</li>
     * <li>Maximum encoded length: writeByte(1) + writeSize(1) + (writeByte(1) +
     * writeShort(2) * 8) = 19 bytes</li>
     * </ul>
     */
    public static BlockHeaderData v1(ForkSignalSet forkSignalSet) {
        return new BlockHeaderData((byte) 0x01, forkSignalSet);
    }

    /**
     * Unrecognized version of header data for preserving later versions of header
     * data in database. It is composed with a version byte and an arbitrary length
     * byte array.
     */
    public static BlockHeaderData unrecognized(byte version, byte[] reserved) {
        return new BlockHeaderData(version, reserved);
    }

    private BlockHeaderData(Byte version, ForkSignalSet forkSignalSet) {
        this.version = version;
        this.forkSignalSet = forkSignalSet;
        this.reserved = Bytes.EMPTY_BYTES;
    }

    private BlockHeaderData(Byte version, byte[] reserved) {
        this.version = version;
        this.forkSignalSet = new ForkSignalSet();
        this.reserved = reserved;
    }

    public boolean signalingFork(ValidatorActivatedFork fork) {
        return forkSignalSet != null && forkSignalSet.signalingFork(fork);
    }

    public byte[] toBytes() {
        if (version == 0x00) {
            return new byte[0];
        } else if (version == 0x01) {
            SimpleEncoder simpleEncoder = new SimpleEncoder();
            simpleEncoder.writeByte(version);
            simpleEncoder.writeBytes(forkSignalSet.toBytes());
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

        if (bytes.length > MAX_SIZE) {
            throw new UnreachableException("Block header data should never be longer than 32 bytes.");
        }

        try {
            SimpleDecoder simpleDecoder = new SimpleDecoder(bytes);
            Byte versionDecoded = simpleDecoder.readByte();
            if (versionDecoded == 0x00) {
                return VERSION_0_HEADER_DATA;
            } else if (versionDecoded == 0x01) {
                ForkSignalSet forkSignalSetDecoded = new ForkSignalSet(simpleDecoder.readBytes());
                return BlockHeaderData.v1(forkSignalSetDecoded);
            } else {
                byte[] reservedDecoded = simpleDecoder.readBytes();
                return BlockHeaderData.unrecognized(versionDecoded, reservedDecoded);
            }
        } catch (Exception ex) {
            logger.trace("Failed to decode BlockHeaderData, falling back to an empty header data. (bytes = {})",
                    Hex.encode(bytes));
            return VERSION_0_HEADER_DATA;
        }
    }

    public static class ForkSignalSet {

        public static final int MAX_SIZE = 1 + 2 * 8;

        public static final int MAX_PENDING_FORKS = 8;

        private final Set<Short> pendingForks;

        private ForkSignalSet() {
            pendingForks = Collections.emptySet();
        }

        public ForkSignalSet(ValidatorActivatedFork... validatorActivatedForks) {
            if (validatorActivatedForks.length > MAX_PENDING_FORKS) {
                throw new UnreachableException("There must not be more than 8 pending forks.");
            }

            pendingForks = Arrays.stream(validatorActivatedForks).map(f -> f.number).collect(Collectors.toSet());
        }

        public ForkSignalSet(byte[] bytes) {
            SimpleDecoder decoder = new SimpleDecoder(bytes);
            byte numberOfPendingForks = decoder.readByte();
            if (numberOfPendingForks < 0 || numberOfPendingForks > MAX_PENDING_FORKS) {
                throw new UnreachableException("Number of pending forks should always be between 0 and 8");
            }

            pendingForks = new HashSet<>();
            for (int i = 0; i < numberOfPendingForks; i++) {
                pendingForks.add(decoder.readShort());
            }
        }

        boolean signalingFork(ValidatorActivatedFork fork) {
            return pendingForks.contains(fork.number);
        }

        byte[] toBytes() {
            SimpleEncoder encoder = new SimpleEncoder();
            encoder.writeByte((byte) pendingForks.size());
            for (Short pendingFork : pendingForks) {
                encoder.writeShort(pendingFork);
            }
            return encoder.toBytes();
        }
    }
}
