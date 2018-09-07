/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.state;

import org.semux.core.Amount;
import org.semux.crypto.Hex;
import org.semux.util.ByteArray;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;

import java.util.HashMap;
import java.util.Map;

public class Account {

    private static final byte[] EMPTY = new byte[0];
    private final byte[] address;
    private Amount available;
    private Amount locked;
    private long nonce;
    private byte[] code;
    private Map<ByteArray, byte[]> storage;

    /**
     * Creates an account instance.
     * 
     * @param address
     * @param available
     * @param locked
     * @param nonce
     */
    public Account(byte[] address, Amount available, Amount locked, long nonce) {
        this.address = address;
        this.available = available;
        this.locked = locked;
        this.nonce = nonce;

        // default values
        storage = new HashMap<>();
        code = EMPTY;
    }

    public Account(byte[] address, Amount available, Amount locked, long nonce, byte[] code,
            Map<ByteArray, byte[]> storage) {
        this(address, available, locked, nonce);
        this.storage = storage;
        this.code = code;
    }

    /**
     * Serializes this account into byte array.
     * 
     * @return
     */
    public byte[] toBytes() {
        SimpleEncoder enc = new SimpleEncoder();
        enc.writeAmount(available);
        enc.writeAmount(locked);
        enc.writeLong(nonce);
        enc.writeBytes(code);
        enc.writeByteMap(storage);

        return enc.toBytes();
    }

    /**
     * Parses an account from byte array.
     * 
     * @param address
     * @param bytes
     * @return
     */
    public static Account fromBytes(byte[] address, byte[] bytes) {
        SimpleDecoder dec = new SimpleDecoder(bytes);
        Amount available = dec.readAmount();
        Amount locked = dec.readAmount();
        long nonce = dec.readLong();

        byte[] code = null;
        Map<ByteArray, byte[]> storage = null;
        try {
            code = dec.readBytes();
            storage = dec.readByteMap();
        } catch (Exception e) {
            // migration, old accounts may not have these fields
        }

        return new Account(address, available, locked, nonce, code, storage);
    }

    /**
     * Returns the address of this account.
     * 
     * @return
     */
    public byte[] getAddress() {
        return address;
    }

    /**
     * Returns the available balance of this account.
     * 
     * @return
     */
    public Amount getAvailable() {
        return available;
    }

    /**
     * Sets the available balance of this account.
     * 
     * @param available
     */
    void setAvailable(Amount available) {
        this.available = available;
    }

    /**
     * Returns the locked balance of this account.
     * 
     * @return
     */
    public Amount getLocked() {
        return locked;
    }

    /**
     * Sets the locked balance of this account.
     * 
     * @param locked
     */
    void setLocked(Amount locked) {
        this.locked = locked;
    }

    /**
     * Gets the nonce of this account.
     * 
     * @return
     */
    public long getNonce() {
        return nonce;
    }

    /**
     * Sets the nonce of this account.
     * 
     * @param nonce
     */
    void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public byte[] getCode() {
        return code;
    }

    public void setCode(byte[] code) {
        this.code = code;
    }

    public Map<ByteArray, byte[]> getStorage() {
        return storage;
    }

    public void setStorage(Map<ByteArray, byte[]> storage) {
        this.storage = storage;
    }

    @Override
    public String toString() {
        return "Account [address=" + Hex.encode(address) + ", available=" + available + ", locked=" + locked
                + ", nonce=" + nonce + "]";
    }
}
