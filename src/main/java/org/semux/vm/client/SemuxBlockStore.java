package org.semux.vm.client;

import org.ethereum.vm.client.BlockStore;
import org.semux.core.Blockchain;

/**
 * Facade class for Blockchain to Blockstore
 *
 * Eventually we'll want to make blockchain just implement blockstore
 */
public class SemuxBlockStore implements BlockStore {
    Blockchain blockchain;

    public SemuxBlockStore(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public byte[] getBlockHashByNumber(int index) {
        return blockchain.getBlock(index).getHash();
    }
}
