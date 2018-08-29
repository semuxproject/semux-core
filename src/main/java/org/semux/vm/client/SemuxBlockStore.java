package org.semux.vm.client;

import org.ethereum.vm.client.BlockStore;
import org.semux.core.Blockchain;

public class SemuxBlockStore implements BlockStore {
    Blockchain blockchain;

    @Override
    public byte[] getBlockHashByNumber(int index) {
        return blockchain.getBlock(index).getHash();
    }
}
