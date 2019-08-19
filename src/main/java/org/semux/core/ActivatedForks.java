/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * An in-memory structure holding all the activated forks.
 */
public class ActivatedForks {

    private static final Logger logger = LoggerFactory.getLogger(ActivatedForks.class);

    private Blockchain chain;
    private Config config;

    /**
     * Activated forks at current height.
     */
    private Map<Fork, Fork.Activation> activatedForks;

    /**
     * Cache of <code>(fork, height) -> activated blocks</code>. As there's only one
     * fork in this version, 2 slots are reserved for current height and current
     * height - 1.
     */
    private Cache<ImmutablePair<Fork, Long>, ForkActivationMemory> forkActivationMemoryCache = Caffeine
            .newBuilder()
            .maximumSize(2)
            .build();

    /**
     * Creates a activated fork set.
     *
     * @param chain
     * @param config
     * @param activatedForks
     */
    public ActivatedForks(Blockchain chain, Config config, Map<Fork, Fork.Activation> activatedForks) {
        this.chain = chain;
        this.config = config;
        this.activatedForks = new ConcurrentHashMap<>(activatedForks);
    }

    /**
     * Tries to activate a fork.
     *
     * @param fork
     */
    public boolean activateFork(Fork fork) {
        Pair<Long, Long> period = config.spec().getForkSignalingPeriod(fork);

        long number = chain.getLatestBlockNumber();
        if (number >= period.getLeft()
                && number <= period.getRight()
                && !isActivated(fork, number)
                && isActivated(fork, number + 1)) {
            activatedForks.put(fork, new Fork.Activation(fork, number + 1));
            logger.info("Fork {} has been activated; it wil be effective from #{}", fork, number + 1);
            return true;
        }

        return false;
    }

    /**
     * Checks if a fork is activated at a certain height of this blockchain.
     *
     * @param fork
     *            An instance of ${@link Fork} to check.
     * @param number
     *            The current block number
     * @return
     */
    public boolean isActivated(Fork fork, final long number) {
        // checks whether the fork has been activated and recorded in database
        if (activatedForks.containsKey(fork)) {
            return number >= activatedForks.get(fork).effectiveFrom;
        }

        // checks whether the local blockchain has reached the fork activation
        // checkpoint
        if (config.manuallyActivatedForks().containsKey(fork)) {
            return number >= config.manuallyActivatedForks().get(fork);
        }

        // returns memoized result of fork activation lookup at current height
        ForkActivationMemory currentHeightActivationMemory = forkActivationMemoryCache
                .getIfPresent(ImmutablePair.of(fork, number));
        if (currentHeightActivationMemory != null) {
            return currentHeightActivationMemory.activatedBlocks >= fork.blocksRequired;
        }

        // search:
        // from (number - 1)
        // to (number - fork.blocksToCheck)
        final long higherBound = Math.max(0, number - 1);
        final long lowerBound = Math.max(0, number - fork.blocksToCheck);
        long activatedBlocks = 0;

        // O(1) dynamic-programming lookup, see the definition of ForkActivationMemory
        ForkActivationMemory forkActivationMemory = forkActivationMemoryCache
                .getIfPresent(ImmutablePair.of(fork, number - 1));
        if (forkActivationMemory != null) {
            activatedBlocks = forkActivationMemory.activatedBlocks -
                    (forkActivationMemory.lowerBoundActivated && lowerBound > 1 ? 1 : 0) +
                    (chain.getBlockHeader(higherBound).getDecodedData().signalingFork(fork) ? 1 : 0);
        } else { // O(m) traversal lookup
            for (long i = higherBound; i >= lowerBound; i--) {
                activatedBlocks += chain.getBlockHeader(i).getDecodedData().signalingFork(fork) ? 1 : 0;
            }
        }

        // memorizes
        forkActivationMemoryCache.put(
                ImmutablePair.of(fork, number),
                new ForkActivationMemory(
                        chain.getBlockHeader(lowerBound).getDecodedData().signalingFork(fork),
                        activatedBlocks));

        // returns
        boolean activated = activatedBlocks >= fork.blocksRequired;
        if (activatedBlocks > 0) {
            logger.debug("Fork: name = {}, requirement = {} / {}, progress = {}",
                    fork.name(), fork.blocksRequired, fork.blocksToCheck, activatedBlocks);
        }

        return activated;
    }

    /**
     * Returns all the activate forks.
     *
     * @return
     */
    public Map<Fork, Fork.Activation> getActivatedForks() {
        return new HashMap<>(activatedForks);
    }

    /**
     * <code>
     * ForkActivationMemory[height].lowerBoundActivated =
     * forkActivated(height - ${@link Fork#blocksToCheck})
     *
     * ForkActivationMemory[height].activatedBlocks =
     * ForkActivationMemory[height - 1].activatedBlocks -
     * ForkActivationMemory[height - 1].lowerBoundActivated ? 1 : 0 +
     * forkActivated(height - 1) ? 1 : 0
     * </code>
     */
    private static class ForkActivationMemory {

        /**
         * Whether the fork is activated at height
         * <code>(current height -{@link Fork#blocksToCheck})</code>.
         */
        public final boolean lowerBoundActivated;

        /**
         * The number of activated blocks at the memorized height.
         */
        public final long activatedBlocks;

        public ForkActivationMemory(boolean lowerBoundActivated, long activatedBlocks) {
            this.lowerBoundActivated = lowerBoundActivated;
            this.activatedBlocks = activatedBlocks;
        }
    }
}
