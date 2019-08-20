/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a block header data.
 */
public class BlockHeaderData {

    public static final int MAX_SIZE = 32;

    private static final Logger logger = LoggerFactory.getLogger(BlockHeaderData.class);

    private final byte[] raw;

    public BlockHeaderData() {
        this(Bytes.EMPTY_BYTES);
    }

    public BlockHeaderData(byte[] raw) {
        if (raw == null || raw.length > MAX_SIZE) {
            throw new IllegalArgumentException("Invalid header data");
        }
        this.raw = raw.clone();
    }

    public BlockHeaderData(ForkSignalSet forks) {
        SimpleEncoder encoder = new SimpleEncoder();
        encoder.writeByte((byte) 0x01);
        encoder.writeBytes(forks.toBytes());
        this.raw = encoder.toBytes();
    }

    /**
     * Parse the fork signal set in this header, in a passive and exception-free
     * way.
     *
     * @return a fork signal set
     */
    public ForkSignalSet parseForkSignals() {
        if (raw.length > 0 && raw[0] == 0x01) {
            try {
                SimpleDecoder decoder = new SimpleDecoder(raw);
                decoder.readByte();
                return ForkSignalSet.fromBytes(decoder.readBytes());
            } catch (Exception e) {
                logger.debug("Failed to parse fork signals", e);
            }
        }

        return ForkSignalSet.of();
    }

    public byte[] toBytes() {
        return this.raw.clone();
    }
}
