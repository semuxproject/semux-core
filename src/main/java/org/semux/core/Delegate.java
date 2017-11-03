/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.Arrays;

import org.semux.utils.Bytes;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;

public class Delegate {
    protected byte[] address;
    protected byte[] name;
    protected long registeredAt;
    protected long votes;

    protected volatile long votesFromMe;

    protected volatile long numberOfBlocksForged;
    protected volatile long numberOfTurnsHit;
    protected volatile long numberOfTurnsMissed;

    /**
     * Create a delegate instance.
     * 
     * @param address
     * @param name
     * @param registeredAt
     * @param votes
     */
    public Delegate(byte[] address, byte[] name, long registeredAt, long votes) {
        this.address = address;
        this.name = name;
        this.registeredAt = registeredAt;
        this.votes = votes;
    }

    public byte[] getAddress() {
        return address;
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

    /**
     * Serializes this delegate object into byte array.
     * 
     * @return
     */
    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeBytes(address);
        enc.writeBytes(name);
        enc.writeLong(registeredAt);
        enc.writeLong(votes);

        return enc.toBytes();
    }

    /**
     * Parses a delegate from a byte array.
     * 
     * @param bytes
     * @return
     */
    public static Delegate fromBytes(byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        byte[] address = dec.readBytes();
        byte[] name = dec.readBytes();
        long registeredAt = dec.readLong();
        long votes = dec.readLong();

        return new Delegate(address, name, registeredAt, votes);
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
        return "Delegate [addr=" + Arrays.toString(address) + ", name=" + Arrays.toString(name) + ", registeredAt="
                + registeredAt + ", votes=" + votes + ", votesFromMe=" + votesFromMe + ", numberOfBlocksForged="
                + numberOfBlocksForged + ", numberOfTurnsHit=" + numberOfTurnsHit + ", numberOfTurnsMissed="
                + numberOfTurnsMissed + "]";
    }
}
