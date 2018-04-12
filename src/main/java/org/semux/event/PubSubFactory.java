/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.event;

import java.util.concurrent.ConcurrentHashMap;

public class PubSubFactory {

    private static final PubSub defaultInstance = new PubSub();

    private static final ConcurrentHashMap<String, PubSub> instances = new ConcurrentHashMap<>();

    public static PubSub getDefault() {
        return defaultInstance;
    }

    public static PubSub get(String name) {
        return instances.computeIfAbsent(name, k -> new PubSub());
    }
}
