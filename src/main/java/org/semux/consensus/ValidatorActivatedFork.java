/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

/**
 * This class represents a Validator Activated Soft Fork inspired by Miner
 * Activated Soft Fork (MASF) in Bitcoin. See:
 * https://github.com/bitcoin/bips/blob/master/bip-0034.mediawiki
 */
public final class ValidatorActivatedFork {

    /**
     * This soft fork introduces an uniformly-distributed hash function for choosing
     * primary validator. See: https://github.com/semuxproject/semux/issues/620
     */
    public static final ValidatorActivatedFork UNIFORM_DISTRIBUTION = new ValidatorActivatedFork((short) 1,
            "UNIFORM_DISTRIBUTION", 1900, 2000);

    /**
     * ${@link SemuxBft} will lookup for the
     * ${@link org.semux.core.BlockHeaderData#forkNumber} of past
     * ${@link ValidatorActivatedFork#activationBlocksLookup} blocks when it enters
     * a new height. ${@link SemuxBft} should consider this fork as activated if
     * there are at least ${@link ValidatorActivatedFork#activationBlocks} blocks
     * containing a fork number that is greater than or equal to the number of this
     * fork.
     */
    public final short number;

    /**
     * The name of this fork.
     */
    public final String name;

    /**
     * The number of blocks which are required to have a fork number that is greater
     * than or equal to the number of this fork.
     */
    public final long activationBlocks;

    /**
     * The number of blocks to lookup for
     * ${@link org.semux.core.BlockHeaderData#forkNumber}.
     */
    public final long activationBlocksLookup;

    private ValidatorActivatedFork(short number, String name, long activationBlocks, long activationBlocksLookup) {
        this.number = number;
        this.name = name;
        this.activationBlocks = activationBlocks;
        this.activationBlocksLookup = activationBlocksLookup;
    }
}
