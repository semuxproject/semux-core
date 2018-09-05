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

    private final BlockHeader blockHeader;
    private static final BigInteger GAS_LIMIT = new BigInteger("999999999");

    public SemuxBlock(BlockHeader block) {
        this.blockHeader = block;
    }

    @Override
    public BigInteger getGasLimit() {
        // TODO - deterministic gas limit
        return GAS_LIMIT;
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
}
