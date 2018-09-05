/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm.client;

import org.ethereum.vm.client.Block;

import java.math.BigInteger;

/**
 * Facade for Block -> Block
 */
public class SemuxBlock implements Block {

    org.semux.core.Block block;
    private static final BigInteger GAS_LIMIT = new BigInteger("999999999");

    public SemuxBlock(org.semux.core.Block block) {
        this.block = block;
    }

    @Override
    public BigInteger getGasLimit() {
        // TODO - deterministic gas limit
        return GAS_LIMIT;
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
