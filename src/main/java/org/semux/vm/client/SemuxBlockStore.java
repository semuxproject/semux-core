/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.client.BlockStore;
import org.semux.core.Blockchain;

/**
 * Facade class for Blockchain to Blockstore
 *
 * Eventually we'll want to make blockchain just implement blockstore
 */
public class SemuxBlockStore implements BlockStore {
    private final Blockchain blockchain;

    public SemuxBlockStore(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public byte[] getBlockHashByNumber(long index) {
        return blockchain.getBlockHeader(index).getHash();
    }
}
