/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

public class Fork {

    public static final Fork UNIFORM_DISTRIBUTION = new Fork(190, 200, 0);

    /**
     * The number of blocks which are required to contain the fork-bit inside ofg
     * ${@link org.semux.core.BlockHeader#data}.
     */
    public final long activationBlocks;

    /**
     * The number of blocks to lookup for the fork-bit inside of
     * ${@link org.semux.core.BlockHeader#data}.
     */
    public final long activationBlocksLookup;

    /**
     * ${@link SemuxBft} will lookup for the fork-bit in the
     * ${@link org.semux.core.BlockHeader} of past
     * ${@link Fork#activationBlocksLookup} blocks when it enters a new height. If
     * the number of blocks containing the fork-bit is greater than or equal to
     * ${@link Fork#activationBlocks}, ${@link SemuxBft} should consider that this
     * fork as activated.
     */
    public final int forkBit;

    public Fork(long activationBlocks, long activationBlocksLookup, int forkBit) {
        this.activationBlocks = activationBlocks;
        this.activationBlocksLookup = activationBlocksLookup;
        this.forkBit = forkBit;
    }
}
