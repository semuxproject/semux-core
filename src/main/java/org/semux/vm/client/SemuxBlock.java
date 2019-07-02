/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.client.Block;
import org.semux.core.BlockHeader;

import java.math.BigInteger;

/**
 * Facade for BlockHeader -> Block
 */
public class SemuxBlock implements Block {

    private final long blockGasLimit;
    private final BlockHeader blockHeader;

    public SemuxBlock(BlockHeader block, long blockGasLimit) {
        this.blockHeader = block;
        this.blockGasLimit = blockGasLimit;
    }

    @Override
    public long getGasLimit() {
        return blockGasLimit;
    }

    @Override
    public byte[] getParentHash() {
        return blockHeader.getParentHash();
    }

    @Override
    public byte[] getCoinbase() {
        return blockHeader.getCoinbase();
    }

    @Override
    public long getTimestamp() {
        return blockHeader.getTimestamp();
    }

    @Override
    public long getNumber() {
        return blockHeader.getNumber();
    }

    @Override
    public BigInteger getDifficulty() {
        return BigInteger.ONE;
    }
}
