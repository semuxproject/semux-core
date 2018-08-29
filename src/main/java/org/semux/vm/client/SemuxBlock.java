package org.semux.vm.client;

import org.ethereum.vm.client.Block;

import java.math.BigInteger;

public class SemuxBlock implements Block {

    org.semux.core.Block block;

    @Override
    public BigInteger getGasLimit() {
        return null;
    }

    @Override
    public byte[] getParentHash() {
        return block.getParentHash();
    }

    @Override
    public byte[] getCoinbase() {
        return block.getCoinbase();
    }

    @Override
    public long getTimestamp() {
        return block.getTimestamp();
    }

    @Override
    public long getNumber() {
        return block.getNumber();
    }
}
