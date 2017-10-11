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

    public Delegate(byte[] addr, byte[] name, long votes) {
        this.addr = addr;
        this.name = name;
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

    public long getVotes() {
        return votes;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public long getVotesFromMe() {
        return votesFromMe;
    }

    public void setVotesFromMe(long votesFromMe) {
        this.votesFromMe = votesFromMe;
    }

    @Override
    public String toString() {
        return "Delegate [addr=" + Arrays.toString(addr) + ", name=" + Arrays.toString(name) + ", votes=" + votes
                + ", votesFromMe=" + votesFromMe + "]";
    }
}
