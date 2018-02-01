/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

public class Fork {

    public static final Fork UNIFORM_DISTRIBUTION = new Fork(190, 200, (short) 1);

    /**
     * The number of blocks which are required to contain the fork number inside of
     * ${@link org.semux.core.BlockHeader#data}.
     */
    public final long activationBlocks;

    /**
     * The number of blocks to lookup for the fork number inside of
     * ${@link org.semux.core.BlockHeader#data}.
     */
    public final long activationBlocksLookup;

    /**
     * ${@link SemuxBft} will lookup for the fork number in the
     * ${@link org.semux.core.BlockHeader} of past
     * ${@link Fork#activationBlocksLookup} blocks when it enters a new height.
     * ${@link SemuxBft} should consider this fork as activated if there are at
     * least ${@link Fork#activationBlocks} blocks containing a fork number that is
     * greater than or equal to the number of this fork.
     */
    public final short number;

    public Fork(long activationBlocks, long activationBlocksLookup, short number) {
        this.activationBlocks = activationBlocks;
        this.activationBlocksLookup = activationBlocksLookup;
        this.number = number;
    }
}
