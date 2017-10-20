/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.Arrays;

import org.semux.utils.Bytes;

public class Delegate {
    protected byte[] addr;
    protected byte[] name;
    protected long registeredAt;
    protected long votes;

    /*
     * Variables below are not persisted
     */
    protected long votesFromMe;
    protected long numberOfBlocksForged;
    protected long numberOfTurnsHit;
    protected long numberOfTurnsMissed;

    public Delegate(byte[] addr, byte[] name, long registeredAt, long votes) {
        this.addr = addr;
        this.name = name;
        this.registeredAt = registeredAt;
        this.votes = votes;
    }

    public byte[] getAddress() {
        return addr;
    }

    public byte[] getName() {
        return name;
    }

    public String getNameString() {
        return Bytes.toString(name);
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public long getVotes() {
        return votes;
    }

    public void setVotes(long votes) {
        this.votes = votes;
    }

    public long getVotesFromMe() {
        return votesFromMe;
    }

    public void setVotesFromMe(long votesFromMe) {
        this.votesFromMe = votesFromMe;
    }

    public long getNumberOfBlocksForged() {
        return numberOfBlocksForged;
    }

    public void setNumberOfBlocksForged(long numberOfBlocksForged) {
        this.numberOfBlocksForged = numberOfBlocksForged;
    }

    public long getNumberOfTurnsHit() {
        return numberOfTurnsHit;
    }

    public void setNumberOfTurnsHit(long numberOfTurnsHit) {
        this.numberOfTurnsHit = numberOfTurnsHit;
    }

    public long getNumberOfTurnsMissed() {
        return numberOfTurnsMissed;
    }

    public void setNumberOfTurnsMissed(long numberOfTurnsMissed) {
        this.numberOfTurnsMissed = numberOfTurnsMissed;
    }

    public double getRate() {
        long total = numberOfTurnsHit + numberOfTurnsMissed;
        return total == 0 ? 0 : numberOfTurnsHit * 100.0 / total;
    }

    @Override
    public String toString() {
        return "Delegate [addr=" + Arrays.toString(addr) + ", name=" + Arrays.toString(name) + ", registeredAt="
                + registeredAt + ", votes=" + votes + ", votesFromMe=" + votesFromMe + ", numberOfBlocksForged="
                + numberOfBlocksForged + ", numberOfTurnsHit=" + numberOfTurnsHit + ", numberOfTurnsMissed="
                + numberOfTurnsMissed + "]";
    }
}
