/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

public enum TransactionType {

    /**
     * (0x00) Coinbase transaction
     */
    COINBASE(0x00),

    /**
     * (0x01) Balance transfer.
     */
    TRANSFER(0x01),

    /**
     * (0x02) Register as a delegate.
     */
    DELEGATE(0x02),

    /**
     * (0x03) Vote for delegate.
     */
    VOTE(0x03),

    /**
     * (0x04) Revoke a previous vote for a delegate.
     */
    UNVOTE(0x04),

    /**
     * (0x05) Create a contract.
     */
    CREATE(0x05),

    /**
     * (0x06) Call a contract.
     */
    CALL(0x06);

    private static final TransactionType[] map = new TransactionType[256];
    static {
        for (TransactionType tt : TransactionType.values()) {
            map[tt.code] = tt;
        }
    }

    public static TransactionType of(byte code) {
        return map[0xff & code];
    }

    private int code;

    TransactionType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public byte toByte() {
        return (byte) code;
    }
}
