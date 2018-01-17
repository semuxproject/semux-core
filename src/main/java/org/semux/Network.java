/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux;

public enum Network {

    MAINNET((byte) 0, "mainnet"),

    TESTNET((byte) 1, "testnet"),

    DEVNET((byte) 2, "devnet");

    Network(byte id, String label) {
        this.id = id;
        this.label = label;
    }

    byte id;
    String label;

    public byte id() {
        return id;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
