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
     * @param height
     */
    public boolean activateFork(Fork fork, long height) {
        if (!isActivated(fork)
                && height <= fork.activationDeadline
                && isActivated(fork, height)) {
            activatedForks.put(fork, new Fork.Activation(fork, height));
            logger.info("Fork {} has been activated at block height {}", fork, height);
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
        // skips genesis block
        if (height <= 1) {
            return false;
        }

        // checks whether the fork has been activated and recorded in database
        if (activatedForks.containsKey(fork)) {
            return height >= activatedForks.get(fork).activatedAt;
        }

        // checks whether the local blockchain has reached the fork activation
        // checkpoint
        if (config.forkActivationCheckpoints().containsKey(fork)) {
            return config.forkActivationCheckpoints().get(fork) <= height;
        }

        // returns memoized result of fork activation lookup at current height
        ForkActivationMemory currentHeightActivationMemory = forkActivationMemoryCache
                .getIfPresent(ImmutablePair.of(fork, height));
        if (currentHeightActivationMemory != null) {
            return currentHeightActivationMemory.activatedBlocks >= fork.activationBlocks;
        }

        // sets boundaries:
        // lookup from (height - 1)
        // to (height - fork.activationBlocksLookup)
        final long higherBound = height - 1;
        final long lowerBound = Math.min(Math.max(height - fork.activationBlocksLookup, 1), higherBound);
        long activatedBlocks = 0;

        // O(1) dynamic-programming lookup, see the definition of ForkActivationMemory
        ForkActivationMemory forkActivationMemory = forkActivationMemoryCache
                .getIfPresent(ImmutablePair.of(fork, height - 1));
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
                ImmutablePair.of(fork, height),
                new ForkActivationMemory(
                        chain.getBlockHeader(lowerBound).getDecodedData().signalingFork(fork),
                        activatedBlocks));

        // returns
        boolean activated = activatedBlocks >= fork.activationBlocks;
        if (activatedBlocks > 0) {
            logger.debug("Fork: name = {}, status = {} / {}, require = {}, activated = {}",
                    fork.name, activatedBlocks, fork.activationBlocksLookup, fork.activationBlocks, activated);
        }

        return activated;
    }

    /**
     * Returns whether a fork has been activated.
     *
     * @param fork
     * @return
     */
    public boolean isActivated(Fork fork) {
        return activatedForks.containsKey(fork);
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
     * forkActivated(height - ${@link Fork#activationBlocksLookup})
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
         * <code>(current height -{@link Fork#activationBlocksLookup})</code>.
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
