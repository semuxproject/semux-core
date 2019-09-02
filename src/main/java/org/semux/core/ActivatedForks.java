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
            logger.info("Fork {} has been activated and will be effective from #{}", fork, number + 1);
            return true;
        }

        return false;
    }

    /**
     * Checks if a fork is activated at a certain height of this blockchain.
     *
     * @param fork
     *            An instance of ${@link Fork} to check.
     * @param height
     *            A blockchain height to check.
     * @return
     */
    public boolean isActivated(Fork fork, final long height) {
        assert (fork.blocksRequired > 0);
        assert (fork.blocksToCheck > 0);

        // checks whether the fork has been activated and recorded in database
        if (activatedForks.containsKey(fork)) {
            return height >= activatedForks.get(fork).effectiveFrom;
        }

        // checks whether the local blockchain has reached the fork activation
        // checkpoint
        if (config.manuallyActivatedForks().containsKey(fork)) {
            return height >= config.manuallyActivatedForks().get(fork);
        }

        // returns memoized result of fork activation lookup at current height
        ForkActivationMemory current = forkActivationMemoryCache.getIfPresent(ImmutablePair.of(fork, height));
        if (current != null) {
            return current.activatedBlocks >= fork.blocksRequired;
        }

        // block range to search:
        // from (number - 1)
        // to (number - fork.blocksToCheck)
        long higherBound = Math.max(0, height - 1);
        long lowerBound = Math.max(0, height - fork.blocksToCheck);
        long activatedBlocks = 0;

        ForkActivationMemory previous = forkActivationMemoryCache.getIfPresent(ImmutablePair.of(fork, height - 1));
        if (previous != null) {
            // O(1) dynamic-programming lookup
            activatedBlocks = previous.activatedBlocks
                    - (lowerBound > 0 && previous.lowerBoundActivated ? 1 : 0)
                    + (chain.getBlockHeader(higherBound).getDecodedData().parseForkSignals().contains(fork) ? 1 : 0);
        } else {
            // O(m) traversal lookup
            for (long i = higherBound; i >= lowerBound; i--) {
                activatedBlocks += chain.getBlockHeader(i).getDecodedData().parseForkSignals().contains(fork) ? 1 : 0;
            }
        }

        logger.trace("number = {}, higher bound = {}, lower bound = {}", height, higherBound, lowerBound);

        // memorizes
        forkActivationMemoryCache.put(ImmutablePair.of(fork, height),
                new ForkActivationMemory(
                        chain.getBlockHeader(lowerBound).getDecodedData().parseForkSignals().contains(fork),
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
