/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core.event;

import org.semux.event.PubSubEvent;

public class BlockchainDatabaseUpgradingEvent implements PubSubEvent {

    public final Long loaded;

    public final Long total;

    public BlockchainDatabaseUpgradingEvent(Long loaded, Long total) {
        this.loaded = loaded;
        this.total = total;
    }
}
