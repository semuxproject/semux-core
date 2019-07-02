/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

/**
 * Validator statistics.
 */
public class ValidatorStats {
    private long blocksForged;
    private long turnsHit;
    private long turnsMissed;

    public ValidatorStats(long forged, long hit, long missed) {
        this.blocksForged = forged;
        this.turnsHit = hit;
        this.turnsMissed = missed;
    }

    public long getBlocksForged() {
        return blocksForged;
    }

    void setBlocksForged(long forged) {
        this.blocksForged = forged;
    }

    public long getTurnsHit() {
        return turnsHit;
    }

    void setTurnsHit(long hit) {
        this.turnsHit = hit;
    }

    public long getTurnsMissed() {
        return turnsMissed;
    }

    void setTurnsMissed(long missed) {
        this.turnsMissed = missed;
    }

    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeLong(blocksForged);
        enc.writeLong(turnsHit);
        enc.writeLong(turnsMissed);
        return enc.toBytes();
    }

    public static ValidatorStats fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        long forged = dec.readLong();
        long hit = dec.readLong();
        long missed = dec.readLong();
        return new ValidatorStats(forged, hit, missed);
    }
}