/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which forks are currently activated.
 */
public final class ActivatedForks {

    private static Map<ValidatorActivatedFork, ValidatorActivatedFork.Activation> activatedForks = new ConcurrentHashMap<>();

    private ActivatedForks() {
    }

    /**
     * Getter for property 'activatedForks'.
     *
     * @return Value for property 'activatedForks'.
     */
    public static synchronized Map<ValidatorActivatedFork, ValidatorActivatedFork.Activation> getActivatedForks() {
        return Collections.unmodifiableMap(activatedForks);
    }

    /**
     * Setter for property 'activatedForks'.
     *
     * @param newForks
     *            Value to set for property 'activatedForks'.
     */
    public static synchronized void setActivatedForks(
            Map<ValidatorActivatedFork, ValidatorActivatedFork.Activation> newForks) {
        if (!activatedForks.equals(newForks)) {
            activatedForks.clear();
            activatedForks.putAll(newForks);
        }
    }

    public static synchronized boolean isActivated(ValidatorActivatedFork fork) {
        return activatedForks.containsKey(fork);
    }

    public static synchronized void addFork(ValidatorActivatedFork fork, ValidatorActivatedFork.Activation activation) {
        activatedForks.put(fork, activation);
    }
}
