/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;
import org.semux.crypto.Hex;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

public class AccountV1 extends Account {

    private final byte[] address;
    private Amount available;
    private Amount locked;
    private long nonce;

    /**
     * Creates an account instance.
     *
     * @param address
     * @param available
     * @param locked
     * @param nonce
     */
    public AccountV1(byte[] address, Amount available, Amount locked, long nonce) {
        this.address = address;
        this.available = available;
        this.locked = locked;
        this.nonce = nonce;
    }

    @Override
    public byte[] getAddress() {
        return address;
    }

    @Override
    public Amount getAvailable() {
        return available;
    }

    @Override
    void setAvailable(Amount available) {
        this.available = available;
    }

    @Override
    public Amount getLocked() {
        return locked;
    }

    @Override
    void setLocked(Amount locked) {
        this.locked = locked;
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    void setNonce(long nonce) {
        this.nonce = nonce;
    }

    @Override
    public String toString() {
        return "Account [address=" + Hex.encode(address) + ", available=" + available + ", locked=" + locked
                + ", nonce=" + nonce + "]";
    }
}
