/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.ethereum.vm.client;

public class BlockStoreMockImpl implements BlockStore {

    @Override
    public byte[] getBlockHashByNumber(int index) {
        return new byte[32];
    }
}
