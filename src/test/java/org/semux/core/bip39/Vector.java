/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.bip39;

public class Vector {

    private byte[] entropy;
    private String mnemonic;
    private String seed;
    private String hdKey;

    public Vector(byte[] entropy, String mnemonic, String seed, String hdKey) {
        this.entropy = entropy;
        this.mnemonic = mnemonic;
        this.seed = seed;
        this.hdKey = hdKey;
    }

    public byte[] getEntropy() {
        return entropy;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public String getSeed() {
        return seed;
    }

    public String getHdKey() {
        return hdKey;
    }
}
