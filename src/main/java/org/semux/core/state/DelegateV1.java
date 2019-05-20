/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import static org.semux.core.Amount.ZERO;

import java.util.Arrays;

import org.semux.core.Amount;
import org.semux.crypto.Hex;
import org.semux.util.Bytes;

/**
 * @deprecated
 */
public class DelegateV1 implements Delegate {
    protected final byte[] address;
    protected final byte[] name;
    protected final long registeredAt;
    protected Amount votes = ZERO;

    /**
     * @deprecated Create a delegate instance.
     *
     * @param address
     * @param name
     * @param registeredAt
     * @param votes
     */
    public DelegateV1(byte[] address, byte[] name, long registeredAt, Amount votes) {
        this.address = address;
        this.name = name;
        this.registeredAt = registeredAt;
        this.votes = votes;
    }

    @Override
    public byte[] getAbyte() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getAddress() {
        return address;
    }

    @Override
    public String getAddressString() {
        return Hex.encode(getAddress());
    }

    @Override
    public byte[] getName() {
        return name;
    }

    @Override
    public String getNameString() {
        return Bytes.toString(name);
    }

    @Override
    public long getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public Amount getVotes() {
        return votes;
    }

    @Override
    public void setVotes(Amount votes) {
        this.votes = votes;
    }

    @Override
    public String toString() {
        return "Delegate [address=" + Hex.encode(address) + ", name=" + Arrays.toString(name) + ", registeredAt="
                + registeredAt + ", votes=" + votes.getNano() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DelegateV1 delegate = (DelegateV1) o;
        return Arrays.equals(address, delegate.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    @Override
    public int compareTo(Delegate o) {
        if (getRegisteredAt() == o.getRegisteredAt()) {
            return org.bouncycastle.util.Arrays.compareUnsigned(getAddress(), o.getAddress());
        } else {
            return Long.compare(getRegisteredAt(), o.getRegisteredAt());
        }
    }
}
